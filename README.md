# Axiom Minestom
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
