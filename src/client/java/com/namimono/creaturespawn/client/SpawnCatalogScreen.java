package com.namimono.creaturespawn.client;

import com.namimono.creaturespawn.command.SpawnCatalog;
import com.namimono.creaturespawn.command.SpawnGroup;
import com.namimono.creaturespawn.command.SpawnQuantity;
import com.namimono.creaturespawn.network.SpawnCatalogC2SPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** 权限命令打开的简易多选刷怪图鉴。 */
public final class SpawnCatalogScreen extends Screen {
	private static final int CELL_WIDTH = 64;
	private static final int CELL_HEIGHT = 62;
	private static final int CELL_GAP = 4;
	private static final int GRID_TOP = 58;
	private static final int MAX_COLUMNS = 8;
	private static final int MAX_ROWS = 3;

	private final Map<SpawnGroup, List<EntityType<?>>> groupedEntries = groupedEntries();
	private final Map<EntityType<?>, LivingEntity> previewEntities = new IdentityHashMap<>();
	private final Set<ResourceLocation> selectedIds = new LinkedHashSet<>();
	private final List<CatalogButton> catalogButtons = new ArrayList<>();
	private SpawnGroup activeGroup = SpawnGroup.HOSTILE;
	private int page;
	private int pageCount = 1;
	private String quantityText = Integer.toString(SpawnQuantity.DEFAULT);
	private EditBox quantityBox;
	private Button spawnButton;
	private Button previousButton;
	private Button nextButton;

	public SpawnCatalogScreen() {
		super(Component.translatable("screen.creature_spawn.spawn_catalog.title"));
	}

	@Override
	protected void init() {
		int tabWidth = Math.min(76, Math.max(48, (width - 28) / SpawnGroup.values().length));
		int tabsWidth = tabWidth * SpawnGroup.values().length;
		int tabX = (width - tabsWidth) / 2;
		int index = 0;
		for (SpawnGroup group : SpawnGroup.values()) {
			Button tab = Button.builder(groupName(group), button -> switchGroup(group))
				.bounds(tabX + index * tabWidth, 30, tabWidth - 2, 20)
				.build();
			tab.active = group != activeGroup;
			addRenderableWidget(tab);
			index++;
		}

		int footerY = height - 28;
		quantityBox = new EditBox(
			font,
			width / 2 - 52,
			footerY,
			36,
			20,
			Component.translatable("screen.creature_spawn.spawn_catalog.quantity")
		);
		quantityBox.setMaxLength(2);
		quantityBox.setFilter(SpawnCatalogScreen::isPotentialQuantity);
		quantityBox.setValue(quantityText);
		quantityBox.setResponder(value -> {
			quantityText = value;
			updateSpawnButton();
		});
		addRenderableWidget(quantityBox);

		spawnButton = Button.builder(
			Component.translatable("screen.creature_spawn.spawn_catalog.spawn"),
			button -> submit()
		).bounds(width / 2, footerY, 72, 20).build();
		addRenderableWidget(spawnButton);

		int pagingY = footerY - 25;
		previousButton = Button.builder(Component.literal("<"), button -> changePage(-1))
			.bounds(width / 2 - 58, pagingY, 24, 20)
			.build();
		nextButton = Button.builder(Component.literal(">"), button -> changePage(1))
			.bounds(width / 2 + 34, pagingY, 24, 20)
			.build();
		addRenderableWidget(previousButton);
		addRenderableWidget(nextButton);

		rebuildCatalogButtons();
		updateSpawnButton();
	}

	private void switchGroup(SpawnGroup group) {
		activeGroup = group;
		page = 0;
		rebuildWidgets();
	}

	private void changePage(int delta) {
		page = Math.max(0, Math.min(pageCount - 1, page + delta));
		rebuildCatalogButtons();
	}

	private void rebuildCatalogButtons() {
		catalogButtons.forEach(this::removeWidget);
		catalogButtons.clear();

		int columns = Math.max(1, Math.min(MAX_COLUMNS, (width - 20) / (CELL_WIDTH + CELL_GAP)));
		int rows = Math.max(1, Math.min(MAX_ROWS, (height - 126) / (CELL_HEIGHT + CELL_GAP)));
		int pageSize = columns * rows;
		List<EntityType<?>> entries = groupedEntries.get(activeGroup);
		pageCount = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
		page = Math.min(page, pageCount - 1);

		int first = page * pageSize;
		int last = Math.min(entries.size(), first + pageSize);
		int gridWidth = columns * CELL_WIDTH + (columns - 1) * CELL_GAP;
		int gridX = (width - gridWidth) / 2;
		for (int entryIndex = first; entryIndex < last; entryIndex++) {
			int localIndex = entryIndex - first;
			EntityType<?> type = entries.get(entryIndex);
			ResourceLocation id = EntityType.getKey(type);
			CatalogButton button = new CatalogButton(
				gridX + (localIndex % columns) * (CELL_WIDTH + CELL_GAP),
				GRID_TOP + (localIndex / columns) * (CELL_HEIGHT + CELL_GAP),
				type,
				previewEntity(type),
				() -> selectedIds.contains(id),
				() -> toggle(id)
			);
			catalogButtons.add(addRenderableWidget(button));
		}

		previousButton.active = page > 0;
		nextButton.active = page + 1 < pageCount;
	}

	private LivingEntity previewEntity(EntityType<?> type) {
		LivingEntity cached = previewEntities.get(type);
		if (cached != null || minecraft == null || minecraft.level == null) {
			return cached;
		}

		Entity created = type.create(minecraft.level);
		if (created instanceof LivingEntity living) {
			previewEntities.put(type, living);
			return living;
		}
		return null;
	}

	private void toggle(ResourceLocation id) {
		if (!selectedIds.add(id)) {
			selectedIds.remove(id);
		}
		updateSpawnButton();
	}

	private void submit() {
		Integer quantity = validQuantity();
		if (selectedIds.isEmpty() || quantity == null) {
			return;
		}
		ClientPlayNetworking.send(new SpawnCatalogC2SPayload(
			List.copyOf(selectedIds),
			new SpawnQuantity(quantity)
		));
	}

	private void updateSpawnButton() {
		if (spawnButton != null) {
			spawnButton.active = !selectedIds.isEmpty() && validQuantity() != null;
		}
	}

	private Integer validQuantity() {
		try {
			int quantity = Integer.parseInt(quantityText);
			return quantity >= SpawnQuantity.MIN && quantity <= SpawnQuantity.MAX ? quantity : null;
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private static boolean isPotentialQuantity(String value) {
		return value.isEmpty() || value.chars().allMatch(Character::isDigit);
	}

	private static Map<SpawnGroup, List<EntityType<?>>> groupedEntries() {
		Map<SpawnGroup, List<EntityType<?>>> grouped = new EnumMap<>(SpawnGroup.class);
		for (SpawnGroup group : SpawnGroup.values()) {
			grouped.put(group, new ArrayList<>());
		}
		for (EntityType<?> type : SpawnCatalog.entries()) {
			grouped.get(SpawnCatalog.group(type)).add(type);
		}
		for (List<EntityType<?>> entries : grouped.values()) {
			entries.sort(Comparator.comparing(type -> EntityType.getKey(type).toString()));
		}
		return grouped;
	}

	private static Component groupName(SpawnGroup group) {
		return Component.translatable(
			"screen.creature_spawn.spawn_catalog.group." + group.name().toLowerCase()
		);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		renderBackground(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
		graphics.drawString(
			font,
			Component.translatable("screen.creature_spawn.spawn_catalog.quantity"),
			width / 2 - 92,
			height - 22,
			0xFFFFFFFF
		);
		graphics.drawCenteredString(
			font,
			Component.translatable(
				"screen.creature_spawn.spawn_catalog.page",
				page + 1,
				pageCount,
				selectedIds.size()
			),
			width / 2,
			height - 47,
			0xFFA0A0A0
		);
		super.render(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static final class CatalogButton extends AbstractButton {
		private static final int MODEL_BOTTOM_OFFSET = 13;
		private static final float MAX_MODEL_SCALE = 26.0F;

		private final EntityType<?> type;
		private final LivingEntity previewEntity;
		private final BooleanSupplier selected;
		private final Runnable onPress;

		private CatalogButton(
			int x,
			int y,
			EntityType<?> type,
			LivingEntity previewEntity,
			BooleanSupplier selected,
			Runnable onPress
		) {
			super(x, y, CELL_WIDTH, CELL_HEIGHT, Component.empty());
			this.type = type;
			this.previewEntity = previewEntity;
			this.selected = selected;
			this.onPress = onPress;
			setTooltip(Tooltip.create(type.getDescription()));
		}

		@Override
		public void onPress() {
			onPress.run();
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
			super.renderWidget(graphics, mouseX, mouseY, delta);
			if (previewEntity != null) {
				renderPreview(graphics, previewEntity);
			}
			renderName(graphics, previewEntity == null);
			if (selected.getAsBoolean()) {
				graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFF00);
			}
		}

		private void renderPreview(GuiGraphics graphics, LivingEntity entity) {
			int modelBottom = getY() + getHeight() - MODEL_BOTTOM_OFFSET;
			float availableWidth = getWidth() - 10.0F;
			float availableHeight = getHeight() - MODEL_BOTTOM_OFFSET - 4.0F;
			float scale = Math.min(
				MAX_MODEL_SCALE,
				Math.min(
					availableWidth / Math.max(0.1F, entity.getBbWidth()),
					availableHeight / Math.max(0.1F, entity.getBbHeight())
				)
			);
			float centerX = getX() + getWidth() / 2.0F;
			float centerY = (getY() + modelBottom) / 2.0F;
			InventoryScreen.renderEntityInInventoryFollowsMouse(
				graphics,
				getX() + 2,
				getY() + 2,
				getX() + getWidth() - 2,
				modelBottom,
				Math.max(1, Math.round(scale)),
				0.0F,
				centerX,
				centerY,
				entity
			);
		}

		private void renderName(GuiGraphics graphics, boolean centeredVertically) {
			String name = type.getDescription().getString();
			int maxWidth = getWidth() - 6;
			if (Minecraft.getInstance().font.width(name) > maxWidth) {
				name = Minecraft.getInstance().font.plainSubstrByWidth(name, maxWidth - 6) + "…";
			}
			int y = centeredVertically
				? getY() + (getHeight() - Minecraft.getInstance().font.lineHeight) / 2
				: getY() + getHeight() - Minecraft.getInstance().font.lineHeight - 2;
			graphics.drawCenteredString(
				Minecraft.getInstance().font,
				name,
				getX() + getWidth() / 2,
				y,
				0xFFFFFFFF
			);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, type.getDescription());
		}
	}
}
