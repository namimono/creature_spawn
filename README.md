# Creature Spawn（刷怪图鉴）

Minecraft **Fabric 1.21.1** 模组：给作弊/OP 用的原版活体图鉴。看见图、勾几种、填数量，一次刷到准星前面。

## 用法

权限 2（作弊/OP）：

- `/creature_spawn` — 打开图鉴
- `/creature_spawn <生物id> [数量]` — 直接刷一种，数量 1–16，省略为 1

落点：准星命中方块则刷在该面；对着空气则沿视线约 8 格落地，再铺成网格。

## 开发

- **Java 21**
- Fabric Loader / Fabric API（开发时由 Gradle 自动拉取）

```bash
./gradlew build
./gradlew runClient
```

产物在 `build/libs/`。

## 工程信息

| 项 | 值 |
|----|-----|
| Mod ID | `creature_spawn` |
| 包名 | `com.namimono.creaturespawn` |
| Mappings | Official Mojang |
| Minecraft | 1.21.1 |
