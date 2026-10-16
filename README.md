# MineNorth Pseudo

Mod Forge 1.20.1 : masque les pseudos Minecraft au-dessus des joueurs.

- Joueur normal : aucun nom au-dessus des autres joueurs.
- Staff en mode staff (`minenorth-admin`) : voit le **nom RP** (`minenorth-identite`) au-dessus des joueurs ;
  un joueur sans identité garde son pseudo visible pour le staff.
- Le chat et le TAB (nom RP) restent gérés par `minenorth-identite`.
- Le mod doit être installé sur le serveur **et** les clients. Il dépend de `minenorth-api`.

Build : compiler `minenorth-api` d'abord, puis `./gradlew build` ici.
