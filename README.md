# Ottergame plugin

Ottergames is a Minecraft plugin made for multiplayer events. The game is made of multiples mini-games that quickly chain one after the other, similary to the Wario Ware series.

Les mini-jeux sont rapident et s'enchaine avec de moins en moins de temps de pause plus le jeu dure longtemps. Un systeme compte les points dans un leaderboard pour la partie en cours, mais aussi pour le total de toutes les parties.

## Lancement rapide (Windows)

Le script `launch_minecraft_with_plugin.bat` fait tout automatiquement:
1. build du plugin,
2. installation du plugin dans le serveur local,
3. ouverture du launcher Minecraft.

Ensuite, connecte ton client sur `localhost:25565`.

Le serveur est lance avec le jar Paper `26.1.2-53` telecharge depuis:
`https://fill-data.papermc.io/v1/objects/6934188878fc351e1be5bfba5f2b8c4591224886e4b34e3de09dbec68a351caf/paper-26.1.2-53.jar`

Paper 26.1.2 necessite Java 25 minimum:
`https://docs.papermc.io/misc/java-install`

Si le launcher n'est pas detecte automatiquement, definis la variable d'environnement `MINECRAFT_LAUNCHER` avec le chemin vers `MinecraftLauncher.exe`.
