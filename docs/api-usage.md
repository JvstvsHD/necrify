# How to use the API

## Setup

### Installation

Replace the version with the current version, e.g. 1.2.2. The latest version can be
found in the top right corner right below the git repository name
or [here](https://ci.jvstvshd.de/job/Necrify/lastSuccessfulBuild/).
Note that you only want to use the string after necrify-{platform}- and without the version build number.

#### Gradle (kotlin)

``` kotlin
repositories {
   mavenCentral()
   maven("https://s01.oss.sonatype.org/content/repositories/snapshots") // (1)
}

dependencies {
    implementation("de.jvstvshd.necrify:necrify-api:1.2.2")
}

```

1. Only add this repository if you want to use the latest snapshot version.

#### Gradle (groovy)

``` groovy
repositories {
    mavenCentral()
    maven { url = 'https://s01.oss.sonatype.org/content/repositories/snapshots' } // (1)
}

dependencies {
   implementation 'de.jvstvshd.necrify:necrify-api:1.2.2'
}
```

1. Only add this repository if you want to use the latest snapshot version.

#### Maven

``` xml
<repositories>
    <repository>
        <id>sonatype-snapshots</id>
        <name>Sonatype Snapshots S01</name>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url> <!-- (1) -->
    </repository>
</repositories>

<dependencies>
   <dependency>
      <groupId>de.jvstvshd.necrify</groupId>
      <artifactId>necrify-api</artifactId>
      <version>1.2.2</version>
   </dependency>
</dependencies>
```

1. Only add this repository if you want to use the latest snapshot version.

!!! warning "Snapshot builds"
    Snapshot builds are not stable and may contain bugs. They are not recommended for production use. Use them at
    your own risk and only for testing purposes.

## Concepts
The Necrify API revolves around the `Necrify` interface. It allows you to access the different parts of the plugin like 
the user system. The central concept of Necrify is the `NecrifyUser` which represents a user on the server. This user can
be banned, muted, kicked, etc. The punishments are represented by the `Punishment` interface which is implemented by the
`Ban` and `Mute` classes. These classes represent the different types of punishments that can be issued to a user and offer
methods to change or cancel the punishment.

## First steps
### Obtain an instance of the API
#### Velocity
If you use the Velocity version, you can use the following code snippet to obtain an instance of the API:
```java
    try {
        Necrify necrify = (Necrify) server.getPluginManager()
                .getPlugin("necrify").orElseThrow().getInstance().orElseThrow();
    } catch(NoSuchElementException e) {
        logger.error("Punishment API is not available"); // (1)
    }
```

1. If the plugin is not available (yet), this will notify you about it in the console.

??? tip "Managing the plugin dependencies correctly"
    You need to specify that you depend on Necrify so your plugin loads after Necrify. This is important because
    otherwise, Necrify may not be found since it gets loaded afterwards.
    ```java
        @Plugin(id = "your-plugin", name = "Your Plugin", version = "1.0.0", dependencies = {
            @Dependency(id = "necrify")
        })
    ```

More to come...

#### Punishing a player

All punishments are issued via the target user. For example, banning a player could be done this way:

```java
    //Firstly, obtain the user instance
    NecrifyUser user = api.getUserManager().getUser(uuid).orElseThrow(() -> new NoSuchElementException("User not found"));
    MiniMessage miniMessage = MiniMessage.miniMessage();
    //temporary ban:
    Ban temporaryBan = user.ban(PunishmentDuration.parse(miniMessage.deserialize("<red>You broke the server's rules! Don't cheat!"), PunishmentDuration.parse("1d"))).join();//1d equals 1 day, the duration is relative to the current time until the punishment is imposed.
    //permanent ban:
    Ban permanentBan = user.banPermanent(miniMessage.deserialize("<red>You broke the server's rules again! You are not allowed to join someday again!")).join();
    //The ban instance you get via #join is the punishment that was issued. Note that using #join blocks the current 
    //Thread and since database operations take some time to complete, it is recommended to use #whenComplete or other.
    //You can now use this instance to change or cancel the punishment:
    temporaryBan.cancel().whenComplete((punishment, throwable) -> {
        if(throwable != null) {
            logger.error("An error occurred while cancelling the punishment", throwable);
            return;
        }
        logger.info("The punishment was successfully cancelled");
    });
    //#cancel should always return the same instance that you called the method on
    //Event tough a permanent ban was issued for an indefinite time, you can still change the duration and the reason:        
    permanentBan.change(PunishmentDuration.parse("300d"), miniMessage.deseriaize("<green>Okay, you may join again in 300 days!")).whenComplete((punishment, throwable) -> {
        if(throwable != null) {
            logger.error("An error occurred while changing the punishment", throwable);
            return;
        }
        logger.info("The punishment was successfully changed");
    });
```
Muting a player is similar, just replace 'ban' with 'mute'.<br>
Kicking a player can be done by calling `user.kick(Reason).join();` where it is safe to call #join since there is no
database query done synchronously. This form of punishment cannot be changed nor cancelled as it only lasts a single moment.<br>