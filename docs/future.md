# What's next
## Support for Paper and Fabric
I plan to introduce full support for Paper and Fabric. The current platform-specific code for Velocity does not contain that
much code. There are still some things that could be abstracted and put into a common module like the user system and database management.
The Paper and Fabric versions of Necrify need to be able to sync data between themself and the Velocity version.

## Punishment stages and templates ([View Github Issue here](https://github.com/JvstvsHD/necrify/issues/10))
Punishment templates would be one reason for a punishment identified by an identifier. This identifier would be used to
set the punishment's reason and duration. This would be useful for servers that have a set of rules and want to have a
consistent way of punishing users. This punishment then could be tracked by its identifier, and if the user gets punished 
again with the same identifier, the next stage of the punishment would be applied. This would ease the process of managing
punishments.

## Internal/API features
### Return tasks instead of CompletableFuture
This would allow additional data being available for Necrify tasks like the actor (which could fix 
[this issue](https://github.com/JvstvsHD/necrify/issues/73)). This would also allow for more control over the tasks and
structure of task execution.

### Configuration
Using Configurate would allow for a more structured configuration file and easier access to the configuration values. It
would also allow Necrify to keep the comments when saving the configuration file.

Also, the current system of saving the default punishment reasons in the configuration file is not the best way to do it.