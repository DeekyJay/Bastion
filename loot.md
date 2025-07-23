# Loot
Progression is built around resource acquisition, not direct gear rewards. Players earn and craft their way up through systems like brewing, enchanting, villager trading, and smelting, with mob difficulty and wave structure gating access to those resources.

## Enchanting
``` mermaid
graph TD
  A[Enchanting Setup]
  A --- XP[XP]
  A --- Lapis[Lapis Lazuli]
  A --- Table[Enchanting Table]
  A --- Shelves[15 Bookshelves]

  %% XP Sources
  XP --- XP_Mobs[Mob Kills]
  XP --- XP_Bottles[Experience Bottles]
  XP --- XP_Trades[Villager Trading]

  %% Lapis Source
  Lapis --- Lapis_Source[Drops from Tier 3+ Mobs]

  %% Enchanting Table Craft
  Table --- Book1[Book]
  Table --- Diamond1[Diamonds ×2]
  Table --- Obsidian[Obsidian ×4]

  Book1 --- Leather1[Leather ×1]
  Book1 --- Paper1[Paper ×3]
  Paper1 --- Cane1[Sugar Cane ×3]

  Obsidian --- Obsidian_Source[Drop or Lava + Water]

  %% Bookshelves
  Shelves --- Books[Books ×15]
  Shelves --- Planks[Wood Planks ×90]

  Books --- Leather2[Leather ×15]
  Books --- Paper2[Paper ×45]
  Paper2 --- Cane2[Sugar Cane ×45]

  Planks --- Logs[Logs ×23]

  ```

  ## Villager trades
  ### Emeralds and XP
  ``` mermaid
  graph LR
  A[Emeralds + XP]

  %% Professions that generate emeralds from trades
  A --- Farmer[Farmer Trades]
  A --- Fletcher[Fletcher Trades]
  A --- Cleric[Cleric Trades]
  A --- Fisherman[Fisherman Trades]
  A --- Butcher[Butcher Trades]
  A --- Librarian[Librarian Trades]
  A --- Armorer[Armorer Trades]

  %% Farmer Trades
  Farmer --- Wheat[Wheat]
  Farmer --- Potato[Potatoes]
  Farmer --- Carrot[Carrots]
  Farmer --- Beetroot[Beetroots]
  Farmer --- Pumpkin[Pumpkins]
  Farmer --- Melon[Melons]

  %% Fletcher Trades
  Fletcher --- Stick[Sticks]
  Fletcher --- String[String]
  Fletcher --- Feather[Feathers]
  Fletcher --- Flint[Flint]

  %% Cleric Trades
  Cleric --- RottenFlesh[Rotten Flesh]
  Cleric --- GoldIngot[Gold Ingots]

  %% Fisherman Trades
  Fisherman --- Coal[Coal]
  Fisherman --- Fish[Raw Fish]

  %% Butcher Trades
  Butcher --- RawPork[Raw Porkchop]
  Butcher --- RawChicken[Raw Chicken]
  Butcher --- RawRabbit[Raw Rabbit]
  Butcher --- Coal2[Coal]

  %% Librarian Trades (some give emeralds early)
  Librarian --- Paper[Paper]
  Librarian --- Book[Books]

  %% Armorer Trades
  Armorer --- Coal3[Coal]
  Armorer --- IronIngot[Iron Ingots]
  ```

  ### Items and XP
  ``` mermaid
  graph LR
  Items[Items + XP]

  %% Professions
  Items --> Librarian
  Items --> Cleric
  Items --> Toolsmith
  Items --> Weaponsmith
  Items --> Armorer
  Items --> Fletcher
  Items --> Cartographer

  %% Librarian Trades
  Librarian --> EnchantedBook
  EnchantedBook --> Emeralds
  EnchantedBook --> Book

  Librarian --> NameTag
  NameTag --> Emeralds

  Librarian --> Bookshelf
  Bookshelf --> Emeralds

  Librarian --> Compass
  Compass --> Emeralds

  %% Cleric Trades
  Cleric --> BottleXP
  BottleXP --> Emeralds

  Cleric --> Lapis
  Lapis --> Emeralds

  Cleric --> Glowstone
  Glowstone --> Emeralds

  Cleric --> EnderPearl
  EnderPearl --> Emeralds

  Cleric --> Redstone
  Redstone --> Emeralds

  %% Toolsmith Trades
  Toolsmith --> IronTools
  IronTools --> Emeralds

  Toolsmith --> DiamondTools
  DiamondTools --> Emeralds

  %% Weaponsmith Trades
  Weaponsmith --> IronWeapons
  IronWeapons --> Emeralds

  Weaponsmith --> DiamondWeapons
  DiamondWeapons --> Emeralds

  %% Armorer Trades
  Armorer --> IronArmor
  IronArmor --> Emeralds

  Armorer --> DiamondArmor
  DiamondArmor --> Emeralds

  %% Fletcher Trades
  Fletcher --> Arrows
  Arrows --> Emeralds

  Fletcher --> TippedArrows
  TippedArrows --> Emeralds
  TippedArrows --> TA_Req2[Dragon Breath]

  Fletcher --> BowCrossbow
  BowCrossbow --> Emeralds

  %% Cartographer Trades
  Cartographer --> ExplorerMap
  ExplorerMap --> Emeralds
  ExplorerMap --> Compass

  Cartographer --> Compass2[Compass]
  Compass2 --> Emeralds
  ```

## Brewing
``` mermaid
graph LR
  Brewing[Brewing]

  %% Core Setup
  Brewing --> BrewingStand[Brewing Stand]
  Brewing --> Bottles[Water Bottles]
  Brewing --> Fuel[Blaze Powder]
  Brewing --> Ingredients[Ingredients]

  %% Brewing Stand
  BrewingStand --> BlazeRod[Blaze Rod]

  %% Bottles
  Bottles --> Glass[Glass Bottles]
  Glass --> Sand[Sand]

  %% Fuel
  Fuel --> BlazePowder[Blaze Powder]
  BlazePowder --> BlazeRod2[Blaze Rod]

  %% Ingredient: Base
  Ingredients --> NetherWart[Nether Wart]

  %% Potion Groups
  Ingredients --> Healing[Healing Potions]
  Ingredients --> Speed[Speed Potions]
  Ingredients --> Strength[Strength Potions]
  Ingredients --> FireRes[Fire Resistance]
  Ingredients --> Poison[Poison]
  Ingredients --> NightVision[Night Vision]
  Ingredients --> Weakness[Weakness]
  Ingredients --> WaterBreathing[Water Breathing]
  Ingredients --> Regen[Regeneration]
  Ingredients --> SlowFalling[Slow Falling]

  %% Specific Potion Ingredients
  Healing --> GlisteringMelon[Glistering Melon Slice]
  GlisteringMelon --> Melon[Melon Slice]
  GlisteringMelon --> GoldNugget[Gold Nugget]

  Speed --> Sugar[Sugar]

  Strength --> BlazePowder2[Blaze Powder]

  FireRes --> MagmaCream[Magma Cream]
  MagmaCream --> Slimeball[Slimeball]
  MagmaCream --> BlazePowder3[Blaze Powder]

  Poison --> SpiderEye[Spider Eye]

  NightVision --> GoldenCarrot[Golden Carrot]
  GoldenCarrot --> Carrot[Carrot]
  GoldenCarrot --> GoldNugget2[Gold Nugget]

  Weakness --> FermentedEye[Fermented Spider Eye]
  FermentedEye --> SpiderEye2[Spider Eye]
  FermentedEye --> Sugar2[Sugar]
  FermentedEye --> BrownMushroom[Brown Mushroom]

  WaterBreathing --> Pufferfish[Pufferfish]

  Regen --> GhastTear[Ghast Tear]

  SlowFalling --> PhantomMembrane[Phantom Membrane]
  ```

  ## Smelting

  A furnace can be used to cook food for higher saturation, make glass (for bottles), and smelting for XP. Consider dropping raw food and seeds as common loot and cooked food only as bonus loot. For an XP farm, Nether Quartz Ore and Ancient Debris are best, followed by Cactus and Clay (both clay and cactus are renewable, but clay is easier, it can be traded with villagers and can be dried from mud).

  ## XP progression
  XP sources:
  - villager trades
  - harvesting moss, skulk and quartz
  - mob kills
  - smelting
  - disenchanting
  - potion