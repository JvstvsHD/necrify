# Necrify
test

Necrify is a punishment plugin designed for currently
[Velocity](https://velocitypowered.com) (and
maybe [Paper/BungeeCord in the future](https://github.com/users/JvstvsHD/projects/5)).<br>

## Table of contents

1. [Plugin installation](#plugin-installation)
2. [Mutes (only affects Velocity)](#mutes)
3. [Duration](#duration)
4. [Commands](#commands)
5. [API](#punishment-api)
    * [installation](#installation)
    * [usage](#usage)

## Plugin installation

1. [Download the latest version of the plugin](https://hangar.papermc.io/JvstvsHD/Necrify/versions) or download dev
   builds [here](https://ci.jvstvshd.de/job/Necrify/) (may be unstable or not working)
2. Put the downloaded file into the ```plugins``` folder of your server.
3. (Re-)Start the server.

## Mutes

With the 1.19.1, Minecraft's chat system got
changed ([detailed explanation](https://gist.github.com/kennytv/ed783dd244ca0321bbd882c347892874)).
Since then, it is no longer possible to block chat messages in the ChatEvent of Velocity due to the signed chat
messages.
This is why the chat listener does not block any messages anymore which means mutes are effectively useless. A solution
to this problem is developing an extension plugin for the actual game servers where cancelling these messages is still
possible. Downloads for this paper plugin are found in
the [releases](https://github.com/JvstvsHD/necrify/releases/latest) and also
as [dev builds](https://ci.jvstvshd.de/job/Necrify/) on Jenkins.<br>
For further information about 1.19.1, please refer to
the [official release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-19-1)

## Commands
All commands are registered with the prefix `/necrify`. Moreover, it is possible to register top-level commands too by 
setting `allow-top-level-commands` to true (which is per default)<br>
<b>legend:</b>

- \<arg\> means the argument is required
- \[arg\] means the argument is optional
- player as argument name means the a player name OR uuid is required
- reason means a reason that supports [MiniMessage](https://docs.advntr.dev/minimessage/format.html)
- duration as argument name means a [duration](#duration)

### Command overview

- **/ban \<player\> \[reason\]** bans a player permanently for the given or the default reason
- **/mute \<player\> \[reason\]** mutes a player permanently for the given or the default reason
- **/tempban <player> <duration> [reason]** bans a player for the given duration for the given or the default reason
- **/tempmute <player> <duration> [reason]** mutes a player for the given duration for the given or the default reason
- **/unban <player>** unbans the given player
- **/unmute <player>** unmutes the given player
- **/necrify user \<player\> \<info|delete|whitelist\>** shows either information about a player's punishments and his whitelist status,
  deletes this user including all punishments or inverts his whitelist status (from whitelisted to blacklisted or vice versa) 
- **/necrify punishment \<punishment id\> <cancel|change|info|remove>** cancels/removes, changes or shows information about the
  given punishment(must be a uuid)

### Duration

To be parsed by `PunishmentDuration#parse(String)`, a string must follow this scheme:<br>
[0-9][s, m, h, d]<br>
s - second(s)<br>
m - minute(s)<br>
h - hour(s)<br>
d - day(s)<br>
These value can be composed, all of them can be omitted.<br>
Example: <b>1d12h15m30s</b> means a duration of 1 day, 12 hours, 15 minutes and 30 seconds.

## Punishment API

### Installation

Replace ```{version}``` with the current version, e.g. 1.0.0. The latest version can be found [here](https://ci.jvstvshd.de/job/Necrify/lastSuccessfulBuild/).
Note that you only want to use the string after necrify-{platform}- and without the version build number.

#### Gradle (kotlin)

```kotlin
repositories {
   mavenCentral()
}

depenencies {
   implementation("de.jvstvshd.necrify:necrify-api:{version}")
}
```

#### Gradle (groovy)

```groovy
repositories {
    mavenCentral()
}

dependencies {
   implementation 'de.jvstvshd.necrify:necrify-api:{version}'
}
```

#### Maven

```xml

<dependencies>
   <dependency>
      <groupId>de.jvstvshd.necrify</groupId>
      <artifactId>necrify-api</artifactId>
      <version>{version}</version>
   </dependency>
</dependencies>
```

You can also depend on the plugin modules or common module. In order to do so, replace the artifactId with the desired module name. 
Note that code outside the API module is always subject to change and may not be stable. It is also not designed to allow
access and modifications from outside the plugin itself and is often not documented.

### Usage

#### Obtaining an instance of the api

If the [plugin](#plugin-installation) is used, you can obtain an instance of the api using the following snippet:

```java
    try {
        Necrify api = (Necrify) server.getPluginManager().getPlugin("necrify").orElseThrow().getInstance().orElseThrow();
    } catch(NoSuchElementException e) {
        logger.error("Punishment API is not available");
    }
```

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
