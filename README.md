<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHDisplayEnhanced

*Améliore les items de display avec une boutique et un système de collections.*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale-3aa655?style=for-the-badge" alt="SkyHand Opale">

</div>

---

## Présentation

Améliore les items de display avec une boutique et un système de collections.

Utilisé sur : **Opale**.

## Dépendances

- Optionnelles : `PlaceholderAPI`, `Vault`, `SHNetwork` (plusieurs serveurs de jeu, voir plus bas)

## Commandes

| Commande | Alias | Description | Permission |
|---|---|---|---|
| `/deco` | `/decorations` | Ouvre le menu principal des collections et de la boutique. | — |
| `/shdisplayenhanced reload` | `/shdeco`, `/shdisplay` | Relit `config.yml`, `messages.yml`, `shop.yml` et `collections/*.yml`. Si un fichier est invalide, rien n'est rechargé et le fichier fautif est indiqué. La section `reseau` demande un redémarrage. | `shdisplayenhanced.admin.reload` |
| `/shdisplayenhanced import` | `/shdeco`, `/shdisplay` | Avec SHNetwork : copie `playerdata.db` dans la base du réseau, sans toucher aux décorations déjà présentes. | `shdisplayenhanced.admin.import` |
| `/shdisplayenhanced info <joueur>` | `/shdeco`, `/shdisplay` | Affiche la progression d'un joueur : décorations par série, séries terminées et, avec SHNetwork, récompense déjà donnée. | `shdisplayenhanced.admin.info` |

La console peut utiliser les sous-commandes d'administration.

## Plusieurs serveurs de jeu (SHNetwork)

Sans SHNetwork, ou avec `reseau.actif: false`, rien ne change : les décorations enregistrées sont dans `playerdata.db` (SQLite) de chaque serveur et écrites à la déconnexion.

Avec SHNetwork, la base du réseau remplace `playerdata.db` :

- les décorations d'un joueur sont lues à son arrivée, une fois que le serveur de départ a fini ses écritures (`awaitSaved`) ; avant cela, aucune décoration ne peut être enregistrée dans le menu ;
- chaque décoration est écrite en base au moment où le joueur l'enregistre (`INSERT IGNORE`), plus à la déconnexion ; la sauvegarde de départ n'attend que les écritures en cours ;
- la récompense d'une série est réservée en base (`INSERT IGNORE` dans `displayenhanced_rewards`) : le serveur qui crée la ligne donne la récompense, l'autre ne donne rien. Si la base ne répond pas après trois essais, aucune récompense n'est donnée et la console l'indique ;
- `/shdisplayenhanced import` reprend `playerdata.db` dans la base ; il peut être relancé sans rien écraser.

Tables : `displayenhanced_items` (joueur, décoration, date) et `displayenhanced_rewards` (joueur, série, date), créées au démarrage si l'utilisateur a le droit CREATE (`sql/schema.sql` pour le faire à la main).

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shdisplayenhanced.admin` | Toutes les sous-commandes de `/shdisplayenhanced`. | `op` |
| `shdisplayenhanced.admin.reload` | `/shdisplayenhanced reload`. | `op` |
| `shdisplayenhanced.admin.import` | `/shdisplayenhanced import`. | `op` |
| `shdisplayenhanced.admin.info` | `/shdisplayenhanced info`. | `op` |

`/deco` n'a pas de permission : tous les joueurs l'utilisent.

## Configuration

Fichiers par défaut, avec des valeurs d'exemple. Cliquez sur un fichier pour l'ouvrir :

<pre>
plugins/SHDisplayEnhanced/
├── <a href="src/main/resources/config.yml">config.yml</a>
├── <a href="src/main/resources/messages.yml">messages.yml</a>
├── <a href="src/main/resources/shop.yml">shop.yml</a>
└── collections/
    └── <a href="src/main/resources/collections/ferme.yml">ferme.yml</a>
</pre>

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>
