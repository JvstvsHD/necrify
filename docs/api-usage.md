# How to use the API

## Setup

### Installation

Replace the version with the current version, e.g. 1.2.2. The latest version can be
found in the top right corner right below the git repository name
or [here](https://modrinth.com/plugin/necrify/versions). A list of all available development versions can be found
[here](https://s01.oss.sonatype.org/content/repositories/snapshots/de/jvstvshd/necrify/necrify-api/).

!!! warning "Snapshot and development builds"
    Snapshot and development builds are builds that are created during development. They are published to test new features
    in advance, but may contain unfinished and/or untested features, thus making them not stable. They are not recommended
    for production use. Use them at your own risk and only for testing purposes, whilst keeping a backup.

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

!!! danger "CompletableFuture#join"
    On this page, when a method returns a `CompletableFuture`, there will be a #join call on it. This is done to simplify the
    code and to make it easier to understand. In a real-world scenario, you should use `CompletableFuture#whenComplete` or other
    methods that require a callback to be executed when the future is completed. Using #join or #get blocks the current thread
    and may lead to performance issues, especially in a server environment.

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
    Ban temporaryBan = user.ban(PunishmentDuration.parse(
            miniMessage.deserialize("<red>You broke the server's rules! Don't cheat!"), 
            PunishmentDuration.parse("1d"))).join();
    //1d equals 1 day, the duration is relative to the current time until the punishment is imposed.
    //permanent ban:
    Ban permanentBan = user.banPermanent(
            miniMessage.deserialize("<red>You broke the server's rules again! You are not allowed to join someday again!")
    ).join();
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
    permanentBan.change(PunishmentDuration.parse("300d"), miniMessage.deseriaize("<green>Okay, you may join again in 300 days!"))
            .whenComplete((punishment, throwable) -> {
                if(throwable != null) {
                    logger.error("An error occurred while changing the punishment", throwable);
                    return;
                }
                logger.info("The punishment was successfully changed");
    });
```

??? tip "Loading vs. getting users"
    In this example, UserManager#getUser(UUID) is used to get a user instance. This method only returns a user if the user
    is already loaded. If the user is not loaded, the method will return an empty optional. If you want to load the user
    if it is not already loaded, you can use UserManager#loadUser(UUID) which will load the user from the database if it is
    not already loaded. See more in the [User API paragraph](#user-api).

Muting a player is similar, just replace 'ban' with 'mute'.<br>
Kicking a player can be done by calling `user.kick(Reason).join();` where it is safe to call #join since there is no
database query done synchronously. This form of punishment cannot be changed nor cancelled as it only lasts a single moment.<br>

## User API
As mentioned before, the central concept of Necrify is the `NecrifyUser` which represents a user on the server. This user
can be retrieved through the `UserManager` which is accessible via the `Necrify` instance. The `UserManager` offers methods
to get a user by its UUID, name, or to load a user if it is not already loaded.
!!! example 
    This example shows how to get the user manager from the Necrify instance:
    ```java
    UserManager userManager = necrify.getUserManager();
    ```
### Retrieving users
Retrieving users is split into two ways: #loadUser(...) and #getUser(...). The difference between these two methods is that
\#getUser(...) only returns a user if it is already loaded. If the user is not loaded, the method will return an empty optional.
\#loadUser(...) on the other hand will load the user from the database if it is not already loaded. If the user is already
loaded, the return value of the corresponding #getUser(...) method is returned. If the user is not loaded, the user is loaded
from the database, after which the returned CompletableFuture is completed with the user instance.

=== "#getUser(...)"
    \#getUser(...) only returns a user if it is already loaded. If the user is not loaded, the method will return an empty optional.
    !!! example
        ```java
        Optional<NecrifyUser> user = userManager.getUser(uuid).join(); // (1)
        Optional<NecrifyUser> userByName = userManager.getUser(name).join(); // (2)
        ```
        
        1. This will return the user identified by its UUID if it is already loaded. 
        2. This will return the user identified by its name if it is already loaded.<br><b>Note</b>: This method may still return an empty optional
            if the user's name is not stored.

=== "#loadUser(...)"
    \#loadUser(...) will load the user from the database if it is not already loaded. If the user is already loaded, the return
    value of the corresponding #getUser(...) method is returned. If the user is not loaded, the user is loaded from the database,
    after which the returned CompletableFuture is completed with the user instance.
    !!! example
        ```java
        Optional<NecrifyUser> user = userManager.loadUser(uuid).join(); // (1)
        Optional<NecrifyUser> userByName = userManager.loadUser(name).join(); // (2)
        ```
        
        1. This will return the user identified by its UUID if it is already loaded. If the already is not loaded, the cached instance is returned.
        2. This will return the user identified by its name if it is already loaded. If the already is not loaded, the cached instance is returned.<br>
            <b>Note</b>: This method may still return an empty optional if the user's name is not stored.

You can query user's punishments by calling `NecrifyUser#getPunishments()` which returns a list of all punishments that were
issued to the user. This list is immutable and cannot be modified. If you want to change or cancel a punishment, you need to
use the corresponding methods on the punishment instance.

### Other UserManager operations

You can also use the methods `#createUser(String|UUID)` to create a new user instance. You only need to supply either the
UUID or the name of the user as this method will always fill out the other field. Necrify will create new user instances 
automatically, so typically you don't need to use this method yourself.

!!! danger "Creating a user"
    This method always tries to create a new user instance in the database. At least for the provided plugins, only one
    user can exist per UUID. So, trying to create a user with an already existing UUID will result in an exception.

Instead, is is more safe to use the `#loadOrCreateUser(String|UUID)` methods which will load the user if it is already
existing or create a new user if it is not existing.

Furthermore, you are able to get all users that are currently loaded by calling `#getLoadedUsers()` which returns a list
of all users that are currently loaded. This list is immutable and cannot be modified.

### Users and their data
If you have successfully obtained a user instance, you can access and modify the user's data. You cannot directly modify
the UUID value of a user since this is a unique identifier for the user. You can, however, modify the user's name, 
through calling `NecrifyUser#queryUsername(boolean)`, although this will only update the user name based on the given
UUID. You may use the boolean parameter to determine whether the user's name should be updated in the database as well.
If you choose to set this to false, you will only retrieve the current Minecraft username through Mojang's API.

All methods that can be used to punish a user were mentioned [previously](#punishing-a-player).

Necrify also offers whitelist management. You can check if a user is whitelisted by calling `NecrifyUser#isWhitelisted()`.
To change this value, you can call `NecrifyUser#setWhitelisted(boolean)`. This will change the user's whitelist status
to the desired value. If the player can no longer join and the whitelist is activated, they will be kicked automatically.

If you want to delete a user, you can call `NecrifyUser#delete()`. This will delete the user from the database and
remove it from the cache. This operation is irreversible and cannot be undone. Deleting a user will also delete all
punishments that were issued to the user and all references in punishment logs. These may get replaced with a placeholder
UUID. (Currently planned, implementation will follow in 1.2.3/1.3.0)

## Punishment API
The punishment API is the central part of Necrify. It revolves around the `Punishment` interface which is extended
by the TemporalPunishment interface. This interface is implemented by the `Ban` and `Mute` classes which represent
the different types of punishments that can be issued to a user. The `Punishment` interface offers methods to change
or cancel the punishment. The `TemporalPunishment` interface offers methods to manage punishments with a fixed expiration
whilst `Punishment` offers general management options.

A punishment may have the following properties (required properties are marked):

- [x] Punishment ID (UUID) which is unique for each punishment
- [x] A user reference which is the user that the punishment was issued to
- [x] A reason which is a string that describes why the punishment was issued (in the Necrify plugin, this is serialized through MiniMessage)
- [x] Creation date to control when the punishment gets enacted
- [x] A type which [identifies the punishment type](#punishment-log-types) (e.g. ban, mute, kick)
- [ ] An expiration (fixed only for temporal punishments; technically all have one, but that of permanent punishments is in a couple thousand years)
- [ ] successor punishments (value may be null)

Those properties correspond to their setters and getters in the `Punishment` interface. You can change the reason (and the 
duration for temporal punishment through `#change(...)`) and cancel the punishment through `#cancel()`. The `#punish()` method
gets called when the punishment is issued. This method is called by the user instance when using the `#ban(...)` or `#mute(...)`
methods and should not be called manually, only if you have newly created your own punishment instance.

Moreover, temporal punishments offer 

### Punishment logs
Punishment logs are a way to track all punishments that were issued to a user and changes that were made to the punishment.
This includes the reason, the duration, the type, succession changes and the user that issued the punishment. The punishment logs are immutable
and cannot be modified. You can access the punishment logs through the `Punishment#loadPunishmentLog()` method which returns
an instance of [`PunishmentLog`](https://docs.jvstvshd.de/necrify/javadoc/latest/de/jvstvshd/necrify/api/punishment/log/PunishmentLog.html).
Using its methods, you can retrieve log entries through various ways (depending on type/action, index or latest entry) and also
create new ones.
!!! danger "issuing own punishment log entries"
    Entries of this system are issued automatically when a punishment is created, changed or cancelled. Issuing own entries
    can create inconsistencies in the system and confuse users. It is recommended to only use this system for reading purposes.
    Only create new entries when you are sure that the system is not able to do it itself.

### Punishment durations
A punishment duration stores the expiration of punishments. There are the following three types:
=== "Relative duration"
    This type of duration is relative to the current time. It is used to determine when the punishment should expire by adding
    the duration to the current time. This is useful for temporary punishments and also used in the commands. All relative
    durations become absolute durations when the action they are used for gets executed/stored.
    !!! example
        ```java
        PunishmentDuration duration = PunishmentDuration.parse("1d"); // (1)
        PunishmentDuration relativeDuration = PunishmentDuration.fromMillis(1000); // (2)
        ```
        
        1. This will create a new punishment duration that lasts for one day using the typical duration format also used for user input.
        2. This creates a new punishment duration that lasts for one second. Similar method exists for java.time.Duration.

=== "Fixed/absolute duration"
    Absolute durations only contain a fixed expiration date. All durations become absolute when stored. All durations read
    from database contexts are absolute.
    !!! example
        ```java
        PunishmentDuration duration = PunishmentDuration.from(LocalDateTime.now().plusDays(1)); // (1)
        ```

        1. This will create a new punishment duration that lasts until today plus one day. There exists a similar method for java.sql.Timestamp `#fromTimestamp`.

=== "Permanent duration"
    Permanent durations are used for punishments that do not expire. Internally, their expiration date is set to December 31, 9999
    and therefore is also a fixed duration. This is used for punishments that should last indefinitely.
    !!! example
        ```java
        PunishmentDuration duration = PunishmentDuration.PERMANENT; // (1)
        ```

        1. This will create a new punishment duration that lasts indefinitely.

You can easily convert between relative and absolute durations by calling `PunishmentDuration#absolute()` or `PunishmentDuration#relative()`.
You can also check if a duration is permanent by calling `PunishmentDuration#isPermanent()`. The string representation used
in user input can also be generated by calling `PunishmentDuration#remainingDuration()`. 

Use `PunishmentDuration#ofPunishment(Punishment)` to get the duration of a punishment. This will return the duration of the punishment
if it is a temporal punishment and `PunishmentDuration.PERMANENT` otherwise.

## Templates and stages
First, you need an instance of TemplateManager: `TemplateManager templateManager = necrify.getTemplateManager()`
You can create templates through `TemplateManager#createTemplate` and retrieve them by name through `TemplateManager#getTemplate`.
`TemplateManager#getTemplates()` gives you a list of all templates.

A detailed explanation to all methods and classes can be found in the [Javadocs](https://jd.jvstvshd.de/necrify/latest/de/jvstvshd/necrify/api/template/package-summary.html).


## Events API
Necrify offers an event system that allows you to listen to events that are fired by the plugin. The events are fired, for example,
when a punishment is issued, changed, or cancelled. You can listen to these events through the [`EventDispatcher`](https://docs.jvstvshd.de/necrify/javadoc/latest/de/jvstvshd/necrify/api/event/EventDispatcher.html).
The event dispatcher is accessible through the `Necrify` instance. You can register event listeners by calling `EventDispatcher#register(Object)`:

!!! example "Registering an event listener"
    ```java
    EventDispatcher eventDispatcher = necrify.getEventDispatcher();
    eventDispatcher.register(new YourEventListener());

    eventDispatcher

    class YourEventListener {
        @Subscribe // (1)
        public void onPunishmentIssued(PunishmentPersecutedEvent event) {
            //Your code here
        }
    }
    ```

    1. This annotation is used to mark a method as an event listener. The method must have exactly one parameter which is the event that is fired.
        Note: You have to use the annotation from org.greenrobot.eventbus.Subscribe and not from any other package.

Event listening also works hierarchically regarding event class inheritance. This means that if you register an event listener for a superclass, it will also listen
to events of subclasses. This is useful if you want to listen to all punishment events, for example. In this case, you can use
the `PunishmentEvent` class as the parameter type for the event listener method. Look at the [Javadocs to gain an overview](https://docs.jvstvshd.de/necrify/javadoc/latest/de/jvstvshd/necrify/api/event/package-summary.html)

You can unregister your listener by calling `EventDispatcher#unregister(Object)` and cancel event propagation by calling `EventDisptacher#cancel(NecrifyEvent)`. 

## Extending the API
!!! warning "This introductory part is not implemented completely yet. It will be in 1.3.0."

You can easily extend this API. First, you are able to exchange the implementations of the UserManager and, consequently, the User
interface and message providers through the corresponding setters in the `Necrify` interface. This is useful if you want 
to store additional data for the user or if you want to use a different storage system. Please note that this is only possible
after `NecrifyPreInitializationEvent` has fired and before `NecrifyInitializedEvent` has fired.
!!! tip "Regarding availability"
    On Velocity, the initialisation of the plugin is done at the last possible point. Therefore, you can acquire the Necrify
    API instance directly in the initialization phase and directly register a listener for the PreInitializationEvent.


### Punishment (log) types
A punishment's type is identified by an integer value. The following types are available per default (`StandardPunishmentType`):

- 1: temporary ban
- 2: permanent ban
- 3: temporary mute
- 4: permanent mute
- 5: kick

If you want to create new punishment types, you can use the `PunishmentTypeRegistry` class where you have to register your type
with a unique integer value. This integer value must be unique and not already used by another type. You also have to provide
a `PunishmentFactory` instance that is able to instantiate your punishment type. This factory is used to create new instances
of punishments from a data map (`Map<String, Object>`). Per default, if you have not overwritten other core parts of Necrify,
this contains the `duration` (`PunishmentDuration`), the `reason` (`String`), the user (`NecrifyUser`), the creation date/
`issued_at` (`LocalDateTime`), the `punishmentUuid` (`UUID`) and the `successor` (`UUID`) if available. Inconsistencies in
this naming might get fixed in future versions, but with backwards compatibility in mind.