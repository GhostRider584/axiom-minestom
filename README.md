# Axiom for Minestom
![Java](https://img.shields.io/badge/Java-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![Minestom](https://img.shields.io/badge/Minestom-FF7175?style=for-the-badge&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA5MzAuMjUgOTMwLjI1Ij48Zz48cGF0aCBkPSJNNDQ3IDQ0N2gzNi4yNXYzNi4yNUg0NDd6bS0yMDUuNTUtMThoNzIuMjV2NzIuMjVoLTcyLjI1em00NDcuMzQgNzIuMjVoLTcyLjI1VjQyOWg3Mi4yNXptLTE4Ny41NC0yNTkuOHY3Mi4yNUg0Mjl2LTcyLjI1ek00MjkgNjg4Ljc5di03Mi4yNWg3Mi4yNXY3Mi4yNXptLTk2LjQzLTQ2Ni4yMXYxMDkuOTlIMjIyLjU4VjIyMi41OHptMzc1LjA5IDM3NS4xdjEwOS45OUg1OTcuNjdWNTk3LjY4em0tNDExLjc3IDczLjMxaC0zNi42NHYtMzYuNjRoMzYuNjR6bTM3NS4xLTM3NS4xaC0zNi42NHYtMzYuNjRoMzYuNjR6TTUxOS44OSAzNy4yMXYxMDkuMTFINDEwLjc4VjM3LjIxek0zNTIuMjQgMTcuMzJ2MTQ4Ljg5SDIwMy4zNVYxNy4zMnpNNjg4LjggMTI3LjY4aC03MS44MlY1NS44Nmg3MS44MnpNNDEwLjM1IDg5My4wNFY3ODMuOTNoMTA5LjExdjEwOS4xMXptMTY3LjY2IDE5Ljg5Vjc2NC4wNEg3MjYuOXYxNDguODl6TTI0MS40NSA4MDIuNTdoNzEuODJ2NzEuODJoLTcxLjgyem02NTEuNTktMjgzLjFINzgzLjkzVjQxMC4zNmgxMDkuMTF6bTE5Ljg5IDIwNy40NEg3NjQuMDRWNTc4LjAyaDE0OC44OXptLTM4LjU0LTQ4NS40NnY3MS44MmgtNzEuODJ2LTcxLjgyek0zNy4yMSA0MTAuNzhoMTA5LjExdjEwOS4xMUgzNy4yMXpNMTcuMzIgMjAzLjM0aDE0OC44OXYxNDguODlIMTcuMzJ6bTM4LjUzIDQ4NS40NXYtNzEuODJoNzEuODJ2NzEuODJ6TTE4My41NCAwdjE4My41NEgwVjB6bTc0Ni43MSA3NDYuNzF2MTgzLjU0SDc0Ni43MVY3NDYuNzF6TTg1Ni44IDExMC4wOWgtMzYuNjRWNzMuNDVoMzYuNjR6TTExMC4wOSA4NTYuOEg3My40NXYtMzYuNjRoMzYuNjR6IiBzdHlsZT0iZmlsbDojZmZmIi8+PC9nPjwvc3ZnPg==)
![Version](https://img.shields.io/badge/version-0.0.1-248cd6?labelColor=&style=for-the-badge)
![License: MIT](https://img.shields.io/badge/License-MIT-7267db.svg?style=for-the-badge)

A server-side integration library that enables [Minestom](https://github.com/Minestom/Minestom) servers
to work seamlessly with the [Axiom](https://axiom.moulberry.com/) mod.

## Overview
Axiom is a powerful client-side mod that provides advanced world editing tools,
3D annotations, region markers, and building capabilities.

This library implements the server-side protocol and functionality needed to
support Axiom clients on Minestom servers.

## Quick Start

### 1. Add Dependency
```kotlin
repositories {
    maven("https://repo.smolder.cloud/public/")
}

dependencies {
    implementation("fr.ghostrider584:axiom-minestom:0.0.1")
}
```

### 2. Initialize Axiom
```java
import fr.ghostrider584.axiom.AxiomMinestom;

public class MyServer {
    public static void main(String[] args) {
        var server = MinecraftServer.init();
        
        // Initialize Axiom support
        AxiomMinestom.initialize();
        
        // Your server setup...
        server.start("0.0.0.0", 25565);
    }
}
```

### 3. Configure Permissions (Optional)
```java
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;

// Set custom permission logic
AxiomPermissions.setPermissionPredicate((player, permission) -> {
    // Your permission logic here
    if (player instanceof PermissionPlayer permissionPlayer) {
        return permissionPlayer.hasPermission(permission.getPermissionNode());
    }
    return false;
});
```

## Events

Listen to Axiom-specific events:

```java
eventNode.addListener(AxiomSpawnEntityEvent.class, event -> {
    Player player = event.getPlayer();
    Entity entity = event.spawnedEntity();
    
    if (entity.getEntityType() == EntityType.PIG) {
        event.setCancelled(true);
    }
});
```

## Demo Server
Check out the included demo server in `demo-server/` for an example implementation.

## Contributing
Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests.

## License
This project is licensed under the MIT License.
