# ArtConnect Pro

ArtConnect Pro est une application JavaFX connectee a une base MySQL. Elle permet de gerer une plateforme artistique locale : artistes, oeuvres, galeries, expositions, workshops, membres, inscriptions, avis, communautes et espace admin.

## Prerequis

- Java 17 ou plus
- Maven
- MySQL 8 ou compatible

## Structure du rendu

Depuis le dossier principal du projet :

```text
GR2_Kouadio_Kabore_Arshdeep_Guezo/
|-- ArtConnectPro-App/      Application JavaFX / Maven
|-- SQL/                    Scripts SQL de creation, donnees et objets avances
```

## Preparation de la base MySQL

Les scripts SQL sont dans le dossier `SQL/`.

Ordre recommande :

```text
1. 01_tables.sql
2. 02_donnees.sql
3. 03_views.sql
4. 04_index.sql
5. 05_routines.sql
6. 06_autorisations.sql
7. 08_community_forum.sql
8. 07_transaction_scenarios.sql (optionnel, pour demonstration et tests)
```

Exemple d'execution depuis le dossier principal :

```bash
mysql -u root -p < SQL/01_tables.sql
mysql -u root -p < SQL/02_donnees.sql
mysql -u root -p < SQL/03_views.sql
mysql -u root -p < SQL/04_index.sql
mysql -u root -p < SQL/05_routines.sql
mysql -u root -p < SQL/06_autorisations.sql
mysql -u root -p < SQL/08_community_forum.sql
```

Le script `07_transaction_scenarios.sql` est optionnel. Il sert a montrer des transactions avec `COMMIT` et `ROLLBACK`.

Le fichier `03_views_et_index.sql` est conserve comme ancienne version fusionnee. La version propre a utiliser est `03_views.sql` puis `04_index.sql`.

## Configuration JDBC

La configuration de connexion se trouve ici :

```text
ArtConnectPro-App/src/main/resources/artconnect-db.properties
```

Exemple :

```properties
artconnect.db.url=jdbc:mysql://localhost:3306/artconnect?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Paris
artconnect.db.user=root
artconnect.db.password=your_password
```

Avant de lancer l'application, remplacer `your_password` par le mot de passe MySQL local.

La configuration peut aussi etre surchargee par proprietes JVM ou variables d'environnement.

## Lancer l'application

Depuis le dossier `ArtConnectPro-App` :

```bash
mvn clean javafx:run
```

Pour verifier uniquement la compilation :

```bash
mvn -q -DskipTests compile
```

Maven recree automatiquement le dossier `target/` et telecharge les dependances declarees dans `pom.xml` si elles ne sont pas deja presentes.

## Comptes de demonstration

Le menu de connexion charge des comptes de demonstration depuis la base.

Exemples utilisables apres execution de `02_donnees.sql` :

```text
Admin      : admin1@artconnect.com      / hash_admin1
Promoteur  : promoter1@artconnect.com   / hash_prom1
Artiste    : monet@artconnect.com       / hash_art1
Premium    : alice@artconnect.com       / hash_mem1
Membre     : bob@artconnect.com         / hash_mem2
```

Les mots de passe sont compares directement aux valeurs `password_hash` du script de donnees, afin de garder une demonstration simple dans le cadre du projet.

## Objets SQL avances

Les objets avances demandes dans le projet se trouvent dans les scripts SQL :

- Vues : `SQL/03_views.sql`
- Index : `SQL/04_index.sql`
- Triggers, procedures et fonctions : `SQL/05_routines.sql`
- Droits SQL : `SQL/06_autorisations.sql`
- Transactions : `SQL/07_transaction_scenarios.sql`
- Forum communautaire : `SQL/08_community_forum.sql`

Fonctions appelees par l'application Java :

- `fn_workshop_participant_count`
- `fn_artwork_average_rating`

## Architecture Java

L'application conserve une architecture en couches :

```text
JavaFX UI (FXML + Controllers)
        |
        v
Service Layer
        |
        v
DAO Interfaces
        |
        v
JDBC Persistence
        |
        v
MySQL Database
```

Packages principaux :

- `com.project.artconnect.model` : entites metier
- `com.project.artconnect.dao` : interfaces DAO
- `com.project.artconnect.persistence` : implementations JDBC
- `com.project.artconnect.service` : services metier
- `com.project.artconnect.ui` : controleurs JavaFX
- `com.project.artconnect.config` et `util` : configuration et utilitaires

Par defaut, l'application utilise les services JDBC connectes a MySQL.

Un mode de secours en memoire existe pour certains tests :

```bash
mvn javafx:run -Dartconnect.useInMemory=true
```

## Fonctionnalites principales

- Connexion avec roles : admin, promoteur, artiste, membre premium, membre free
- Consultation des artistes, oeuvres, galeries, expositions et workshops
- CRUD principal sur artistes, oeuvres et expositions
- Inscriptions aux workshops
- Avis sur les oeuvres
- Forum communautaire
- Tableau de bord admin avec statistiques, demandes et audit
- Onglet Discover pour mettre en avant contenus et evenements

## Notes de remise

Ne pas versionner les fichiers generes ou locaux :

- `target/`
- `.idea/`
- `.DS_Store`
- `hs_err_pid*.log`

Ces fichiers ne sont pas necessaires. Maven reconstruit `target/` automatiquement.
