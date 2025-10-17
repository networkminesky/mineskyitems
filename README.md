# MineSky Items
Another Minecraft custom item plugin for RPG servers - Originally designed for minesky.com.br

## Features
- Custom and intuitive menus for creating and editing items in-game
- Support for multiple triggers and integration with MythicMobs skills
- Custom Model Data support — you can use your own resource pack models
- Automatic attribute scaling (e.g., damage, attack speed) based on item level using curve formulas
- Support for multiple item lore for rich item descriptions
- Rarity system with custom font support

## Installing
This plugin is compatible with any PaperMC (or fork) server running Minecraft 1.21.4 or newer.

You can download the latest version from the [Releases](https://github.com/networkminesky/mineskyitems/releases) tab of this repository.

## Adding as a dependency
You can use the default static classes (ItemHandler, CategoryHandler and so on) directly from the plugin to use it as a API.

To use this repository as a dependency in your Maven project:
```xml
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>

<dependency>
  <groupId>com.github.networkminesky</groupId>
  <artifactId>mineskyitems</artifactId>
  <version>1.0.8-ALPHA</version>
</dependency>
```

Or if you are using Gradle:
```.gradle
repositories {
  maven { url "https://jitpack.io"  }
}
dependencies {
  compileOnly("com.github.networkminesky:mineskyitems:1.0.8-ALPHA")
}
```

