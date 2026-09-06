package com.namimono.creaturespawn.client;

import com.namimono.creaturespawn.SpawnCatalogOpenPolicy;
import com.namimono.creaturespawn.command.SpawnCatalog;
import com.namimono.creaturespawn.command.SpawnEntry;
import com.namimono.creaturespawn.command.SpawnGroup;
import com.namimono.creaturespawn.command.SpawnQuantity;
import com.namimono.creaturespawn.network.SpawnCatalogC2SPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** 权限命令打开的简易多选刷怪图鉴。 */
public final class SpawnCatalogScreen extends Screen {
	private static final int CELL_WIDTH = 64;
	private static final int CELL_HEIGHT = 62;
	private static final int CELL_GAP = 4;
	private static final int GRID_TOP = 58;
	private static final int MAX_COLUMNS = 8;
	private static final int MAX_ROWS = 3;

	private final Map<SpawnGroup, List<SpawnEntry>> groupedEntries = groupedEntries();
	private final Map<ResourceLocation, LivingEntity> previewEntities = new HashMap<>();
	private final Set<ResourceLocation> selectedIds = new LinkedHashSet<>();
	private final List<CatalogButton> catalogButtons = new ArrayList<>();
	private SpawnGroup activeGroup = SpawnGroup.HOSTILE;
	private int page;
	private int pageCount = 1;
	private String quantityText = Integer.toString(SpawnQuantity.DEFAULT);
	private boolean lockPose;
	private EditBox quantityBox;
	private Button selectPageButton;
	private Button lockPoseButton;
	private Button spawnButton;
	private Button placeButton;
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
		int selectPageWidth = 76;
		int spawnWidth = 64;
		int lockPoseWidth = 72;
		int quantityLabelWidth = 28;
		int quantityBoxWidth = 36;
		int spacing = 8;
		int totalControlsWidth = selectPageWidth
			+ spacing
			+ quantityLabelWidth
			+ quantityBoxWidth
			+ spacing
			+ lockPoseWidth
			+ spacing
			+ spawnWidth;
		int startX = (width - totalControlsWidth) / 2;

		selectPageButton = Button.builder(
			Component.translatable("screen.creature_spawn.spawn_catalog.select_page"),
			button -> toggleSelectCurrentPage()
		).bounds(startX, footerY, selectPageWidth, 20).build();
		addRenderableWidget(selectPageButton);

		int quantityBoxX = startX + selectPageWidth + spacing + quantityLabelWidth;
		quantityBox = new EditBox(
			font,
			quantityBoxX,
			footerY,
			quantityBoxWidth,
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

		int lockPoseButtonX = quantityBoxX + quantityBoxWidth + spacing;
		lockPoseButton = Button.builder(
			lockPoseLabel(),
			button -> {
				lockPose = !lockPose;
				updateLockPoseButton();
			}
		).bounds(lockPoseButtonX, footerY, lockPoseWidth, 20)
			.tooltip(Tooltip.create(Component.translatable(
				"screen.creature_spawn.spawn_catalog.lock_pose.tooltip"
			)))
			.build();
		addRenderableWidget(lockPoseButton);

		int spawnButtonX = lockPoseButtonX + lockPoseWidth + spacing;
		spawnButton = Button.builder(
			Component.translatable("screen.creature_spawn.spawn_catalog.spawn"),
			button -> submit()
		).bounds(spawnButtonX, footerY, spawnWidth, 20).build();
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
		placeButton = Button.builder(
			Component.translatable("screen.creature_spawn.spawn_catalog.place_mode"),
			button -> beginPlacement()
		).bounds(width / 2 - 160, pagingY, 72, 20)
			.tooltip(Tooltip.create(Component.translatable(
				"screen.creature_spawn.spawn_catalog.place_mode.tooltip"
			)))
			.build();
		addRenderableWidget(placeButton);

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

		int columns = calculateColumns();
		int rows = calculateRows();
		int pageSize = columns * rows;
		List<SpawnEntry> entries = groupedEntries.get(activeGroup);
		pageCount = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
		page = Math.min(page, pageCount - 1);

		int first = page * pageSize;
		int last = Math.min(entries.size(), first + pageSize);
		int gridWidth = columns * CELL_WIDTH + (columns - 1) * CELL_GAP;
		int gridX = (width - gridWidth) / 2;
		for (int entryIndex = first; entryIndex < last; entryIndex++) {
			int localIndex = entryIndex - first;
			SpawnEntry entry = entries.get(entryIndex);
			ResourceLocation id = entry.id();
			CatalogButton button = new CatalogButton(
				gridX + (localIndex % columns) * (CELL_WIDTH + CELL_GAP),
				GRID_TOP + (localIndex / columns) * (CELL_HEIGHT + CELL_GAP),
				entry,
				previewEntity(entry),
				() -> selectedIds.contains(id),
				() -> toggle(id)
			);
			catalogButtons.add(addRenderableWidget(button));
		}

		previousButton.active = page > 0;
		nextButton.active = page + 1 < pageCount;
		updateSelectPageButton();
	}

	private int calculateColumns() {
		return Math.max(1, Math.min(MAX_COLUMNS, (width - 20) / (CELL_WIDTH + CELL_GAP)));
	}

	private int calculateRows() {
		return Math.max(1, Math.min(MAX_ROWS, (height - 126) / (CELL_HEIGHT + CELL_GAP)));
	}

	private List<SpawnEntry> currentPageEntries() {
		List<SpawnEntry> entries = groupedEntries.get(activeGroup);
		if (entries == null || entries.isEmpty()) {
			return List.of();
		}
		int pageSize = calculateColumns() * calculateRows();
		int first = page * pageSize;
		if (first >= entries.size()) {
			return List.of();
		}
		int last = Math.min(entries.size(), first + pageSize);
		return entries.subList(first, last);
	}

	private boolean isCurrentPageAllSelected(List<SpawnEntry> currentEntries) {
		if (currentEntries.isEmpty()) {
			return false;
		}
		for (SpawnEntry entry : currentEntries) {
			if (!selectedIds.contains(entry.id())) {
				return false;
			}
		}
		return true;
	}

	private void toggleSelectCurrentPage() {
		List<SpawnEntry> currentEntries = currentPageEntries();
		if (currentEntries.isEmpty()) {
			return;
		}

		boolean allSelected = isCurrentPageAllSelected(currentEntries);
		for (SpawnEntry entry : currentEntries) {
			ResourceLocation id = entry.id();
			if (allSelected) {
				selectedIds.remove(id);
			} else {
				selectedIds.add(id);
			}
		}

		updateSelectPageButton();
		updateSpawnButton();
	}

	private void updateSelectPageButton() {
		if (selectPageButton == null) {
			return;
		}
		List<SpawnEntry> currentEntries = currentPageEntries();
		if (currentEntries.isEmpty()) {
			selectPageButton.active = false;
			selectPageButton.setMessage(Component.translatable("screen.creature_spawn.spawn_catalog.select_page"));
			return;
		}
		selectPageButton.active = true;
		boolean allSelected = isCurrentPageAllSelected(currentEntries);
		selectPageButton.setMessage(Component.translatable(
			allSelected
				? "screen.creature_spawn.spawn_catalog.deselect_page"
				: "screen.creature_spawn.spawn_catalog.select_page"
		));
	}

	private LivingEntity previewEntity(SpawnEntry entry) {
		LivingEntity cached = previewEntities.get(entry.id());
		if (cached != null || minecraft == null || minecraft.level == null) {
			return cached;
		}

		Entity created = entry.type().create(minecraft.level);
		if (created instanceof LivingEntity living) {
			if (living instanceof Mob mob) {
				entry.prepare(mob);
			}
			previewEntities.put(entry.id(), living);
			return living;
		}
		return null;
	}

	private void toggle(ResourceLocation id) {
		if (!selectedIds.add(id)) {
			selectedIds.remove(id);
		}
		updateSelectPageButton();
		updateSpawnButton();
	}

	private void submit() {
		Integer quantity = validQuantity();
		if (selectedIds.isEmpty() || quantity == null) {
			return;
		}
		ClientPlayNetworking.send(new SpawnCatalogC2SPayload(
			List.copyOf(selectedIds),
			new SpawnQuantity(quantity),
			lockPose,
			false
		));
	}

	private void beginPlacement() {
		Integer quantity = validQuantity();
		if (selectedIds.isEmpty() || quantity == null) {
			return;
		}
		MobPlacementClient.begin(List.copyOf(selectedIds), new SpawnQuantity(quantity), lockPose);
		onClose();
	}

	private void updateLockPoseButton() {
		if (lockPoseButton != null) {
			lockPoseButton.setMessage(lockPoseLabel());
		}
	}

	private Component lockPoseLabel() {
		return Component.translatable(
			lockPose
				? "screen.creature_spawn.spawn_catalog.lock_pose.on"
				: "screen.creature_spawn.spawn_catalog.lock_pose.off"
		);
	}

	private void updateSpawnButton() {
		boolean ready = !selectedIds.isEmpty() && validQuantity() != null;
		if (spawnButton != null) {
			spawnButton.active = ready;
		}
		if (placeButton != null) {
			placeButton.active = ready;
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

	private static Map<SpawnGroup, List<SpawnEntry>> groupedEntries() {
		Map<SpawnGroup, List<SpawnEntry>> grouped = new EnumMap<>(SpawnGroup.class);
		for (SpawnGroup group : SpawnGroup.values()) {
			grouped.put(group, new ArrayList<>());
		}
		for (SpawnEntry entry : SpawnCatalog.entries()) {
			grouped.get(SpawnCatalog.group(entry.type())).add(entry);
		}
		for (List<SpawnEntry> entries : grouped.values()) {
			entries.sort(Comparator.comparing(entry -> entry.id().toString()));
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
		if (!SpawnCatalogOpenPolicy.shouldRender(FlashbackReplay.isActive())) {
			return;
		}
		renderBackground(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
		Component quantityLabel = Component.translatable("screen.creature_spawn.spawn_catalog.quantity");
		graphics.drawString(
			font,
			quantityLabel,
			quantityBox.getX() - font.width(quantityLabel) - 4,
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

		private final SpawnEntry entry;
		private final LivingEntity previewEntity;
		private final BooleanSupplier selected;
		private final Runnable onPress;

		private CatalogButton(
			int x,
			int y,
			SpawnEntry entry,
			LivingEntity previewEntity,
			BooleanSupplier selected,
			Runnable onPress
		) {
			super(x, y, CELL_WIDTH, CELL_HEIGHT, Component.empty());
			this.entry = entry;
			this.previewEntity = previewEntity;
			this.selected = selected;
			this.onPress = onPress;
			setTooltip(Tooltip.create(entry.description()));
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
			String name = entry.description().getString();
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
			output.add(NarratedElementType.TITLE, entry.description());
		}
	}
}
