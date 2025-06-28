# Dependency Diagram
``` mermaid
graph TD
    subgraph "Core Instantiation"
        Bastion
    end

    subgraph "Problematic Managers"
        A[WaveManager]
        B[MobSpawnManager]
        C[LootManager]
    end

    subgraph "Other Managers"
        D[TradeManager]
        E[UpgradeManager]
        F[UIManager]
        G[LightningManager]
        H[WorldListener]
        I[BarrierManager]
        J[VillageManager]
    end

    Bastion --> A
    Bastion --> B
    Bastion --> C
    Bastion --> D
    Bastion --> E
    Bastion --> F
    Bastion --> G
    Bastion --> H
    Bastion --> I
    Bastion --> J


    A -- "Needs to start spawning" --> B
    B -- "Needs to handle loot drops" --> C
    C -- "Needs wave number for loot tiers" --> A

    D --> J
    D --> A
    E --> J
    F --> A
    F --> J
    G --> I
    H --> J
    I --> J


    style A fill:#ff9999,stroke:#333,stroke-width:2px
    style B fill:#ff9999,stroke:#333,stroke-width:2px
    style C fill:#ff9999,stroke:#333,stroke-width:2px


```

``` mermaid
graph TD
    subgraph "Core Instantiation"
        Bastion
    end

    subgraph "State"
        GSM[GameStateManager]
    end

    subgraph "Problematic Managers"
        A[WaveManager]
        B[MobSpawnManager]
        C[LootManager]
    end

    subgraph "Other Managers"
        D[TradeManager]
        E[UpgradeManager]
        F[UIManager]
        G[LightningManager]
        H[WorldListener]
        I[BarrierManager]
        J[VillageManager]
    end

    Bastion --> GSM
    Bastion --> A
    Bastion --> B
    Bastion --> C
    Bastion --> D
    Bastion --> E
    Bastion --> F
    Bastion --> G
    Bastion --> H
    Bastion --> I
    Bastion --> J

    A -- "Needs to start spawning" --> B
    B -- "Needs to handle loot drops" --> C
    
    A -- "Updates wave number in" --> GSM
    C -- "Reads wave number from" --> GSM

    D --> J
    D --> A
    E --> J
    F --> A
    F --> J
    F --> GSM
    G --> I
    G --> GSM
    H --> J
    I --> J

    style C fill:#99ff99,stroke:#333,stroke-width:2px
    style GSM fill:#99ff99,stroke:#333,stroke-width:2px

```
