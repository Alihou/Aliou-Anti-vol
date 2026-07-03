# Sécurité Perso — MVP anti-vol (usage strictement personnel)

App Android native (hors Play Store) + console web (PWA) pour localiser,
verrouiller, déclencher une alarme, prendre une photo discrète, ou effacer
ton propre téléphone en cas de perte ou de vol — par simple SMS, donc
fonctionnel même sans connexion data.

⚠️ **Conçu pour un usage sur ton propre appareil uniquement.** Installer ce
type d'app sur le téléphone de quelqu'un d'autre sans son consentement est
illégal dans la quasi-totalité des juridictions (surveillance non
consentie). Ce projet part du principe que c'est *toi* qui installes l'app
sur *ton propre* téléphone, avant qu'il ne soit perdu ou volé.

---

## 1. Structure du projet

```
antivol-mvp/
├── app/                     ← projet Android Studio (à ouvrir tel quel)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/perso/antivol/
│       │   ├── MainActivity.kt        (écran de configuration)
│       │   ├── SmsReceiver.kt         (écoute les commandes SMS)
│       │   ├── CommandService.kt      (exécute les commandes)
│       │   ├── MyDeviceAdminReceiver.kt
│       │   ├── BootReceiver.kt
│       │   ├── SimChangeReceiver.kt
│       │   ├── LocationHelper.kt
│       │   ├── CameraHelper.kt
│       │   ├── EmailSender.kt
│       │   └── Prefs.kt               (stockage chiffré)
│       └── res/...
└── control-panel/           ← la PWA, à héberger ou ouvrir en local
    ├── index.html
    └── manifest.json
```

## 2. Obtenir l'APK installable — sans Android Studio, juste ton navigateur

Ce projet inclut un fichier `.github/workflows/build-apk.yml` qui demande à
GitHub de compiler l'app automatiquement sur ses propres serveurs, gratuitement.
Toi, tu n'as besoin que d'un compte GitHub et d'un navigateur.

**Étape 1 — Créer un compte et un dépôt**
1. Va sur [github.com](https://github.com) et crée un compte gratuit si tu n'en as pas.
2. Clique sur le **+** en haut à droite → **New repository**.
3. Nomme-le par exemple `antivol-perso`, laisse-le en **Private** (personne d'autre ne verra ton code), clique **Create repository**.

**Étape 2 — Envoyer les fichiers du projet**
1. Sur la page du dépôt vide, clique **uploading an existing file**.
2. Dézippe `antivol-mvp.zip` sur ton ordinateur, puis glisse-dépose **tout le contenu du dossier** `antivol-mvp/` (pas le dossier lui-même, son contenu) dans la zone d'upload de GitHub.
3. Descends en bas de page, clique **Commit changes**.

**Étape 3 — Laisser GitHub compiler**
1. Clique sur l'onglet **Actions** en haut du dépôt.
2. Tu verras "Compiler l'APK" en cours d'exécution (rond jaune/orange qui tourne) — patiente 3 à 5 minutes.
3. Quand le rond devient vert ✅, clique dessus, puis descends jusqu'à **Artifacts** en bas de page.
4. Clique sur `antivol-perso-apk` pour télécharger un fichier `.zip` contenant ton `app-debug.apk`.

**Étape 4 — Installer l'APK sur ton téléphone**
1. Transfère ce `.apk` sur ton téléphone (par email à toi-même, Google Drive, Bluetooth, câble USB...).
2. Ouvre-le depuis ton téléphone (via l'app Fichiers). Android va demander d'autoriser l'installation depuis cette source — accepte.
3. L'app "Sécurité Perso" apparaît dans ton menu d'apps. Ouvre-la et configure-la comme décrit à l'étape 5.

Aucune ligne de commande, aucun IDE, aucun SDK à installer sur ton PC ou ton
téléphone — uniquement le navigateur pour les étapes 1 à 3.

## 3. Configuration dans l'app (une fois installée)

Ouvre l'app "Sécurité Perso" et remplis :
   - ton numéro de téléphone (celui qui aura le droit d'envoyer des commandes)
   - un **PIN long** (6+ chiffres, pas de date de naissance)
   - optionnellement ton email Gmail + un [mot de passe d'application](https://myaccount.google.com/apppasswords) (pour recevoir les photos)

Enregistre, puis fais les 3 étapes dans l'ordre : permissions → administrateur de l'appareil → désactiver l'optimisation de batterie.

Sur certains téléphones (Xiaomi/MIUI, Tecno, Infinix, Oppo...), il existe
aussi un réglage constructeur "Démarrage automatique" ou "Gestion des
apps en arrière-plan" — active-le aussi pour cette app, sinon le
fabricant risque de tuer le service au bout de quelques heures.

## 4. La console de contrôle (PWA)

`control-panel/index.html` est une page web autonome, sans dépendance,
sans backend. Pour l'utiliser depuis un autre téléphone ou un ordinateur :

- **Le plus simple** : ouvre directement le fichier `index.html` dans un
  navigateur (double-clic, ou transfère-le sur ton autre téléphone et
  ouvre-le avec Chrome).
- **Pour y accéder via un lien** (plus pratique) : héberge le dossier
  `control-panel/` gratuitement sur GitHub Pages, Netlify ou Vercel — glisser-déposer
  le dossier suffit sur ces plateformes, pas de configuration serveur nécessaire.

Au premier lancement, renseigne le numéro du téléphone protégé et le même
PIN que dans l'app Android, clique "Enregistrer" (stocké uniquement en
local sur cet appareil de contrôle, jamais envoyé nulle part). Ensuite,
chaque bouton ouvre directement ton app SMS avec la commande pré-remplie —
il ne reste qu'à appuyer sur envoyer.

## 5. Protocole des commandes SMS

Format : `AV:<PIN>:<COMMANDE>[:<ARGUMENT>]`

| Commande | Effet |
|---|---|
| `LOCALISER` | Répond avec un lien Google Maps de la position actuelle |
| `VERROUILLER` | Verrouille l'écran immédiatement |
| `ALARME` | Joue un son fort pendant 60s, même en mode silencieux |
| `PHOTO` | Prend une photo (caméra avant) et l'envoie par email si data dispo |
| `BATTERIE` | Répond avec le niveau de batterie actuel |
| `EFFACER:CONFIRMER` | Efface toutes les données (réinitialisation usine) — irréversible |

Le SMS de commande est automatiquement masqué de la boîte de réception une
fois traité, pour ne pas alerter quelqu'un qui aurait le téléphone en main.

## 6. Sécurité — points à comprendre avant utilisation

- **Le PIN est ta seule protection.** N'importe qui connaissant ton numéro
  pourrait en théorie usurper l'expéditeur d'un SMS sur certains réseaux ;
  le PIN empêche qu'une commande soit exécutée sans le connaître. Choisis-en
  un long et ne le partage avec personne.
- **La commande `EFFACER` est irréversible.** Elle exige le mot `CONFIRMER`
  en plus du PIN précisément pour réduire le risque d'erreur ou de rejeu accidentel.
- **Ce projet n'est volontairement pas sur le Play Store**, donc aucune des
  restrictions de permissions SMS de Google (réservées à l'app SMS par
  défaut) ne s'applique — mais en contrepartie, aucune vérification
  Play Protect automatique ne couvre cette app : maintiens-la toi-même à jour et ne la partage avec personne d'autre.
- **Le point vert/orange caméra-micro à l'écran** lors de la prise de photo
  est imposé par Android lui-même depuis la version 12 et ne peut pas être
  désactivé par l'app — c'est voulu, ça fait partie du contrat de confiance
  d'Android, même pour ce cas d'usage.
- **Sauvegarde ton PIN et ton numéro ailleurs** (papier, gestionnaire de
  mots de passe) — si tu les oublies après avoir perdu le téléphone, tu ne
  pourras plus reconfigurer l'app sur l'appareil perdu.

## 7. Limites connues de ce MVP

- La commande `PHOTO` nécessite une connexion data au moment de l'envoi
  pour transmettre l'image (SMS classique ne transporte pas d'image). Sans
  data, la photo reste stockée localement sur le téléphone volé — non
  récupérable à distance dans cette version.
- `LOCALISER` dépend du GPS/réseau étant activé sur le téléphone — un
  voleur qui désactive totalement la localisation empêchera cette commande.
- Testé conceptuellement pour Android 8 (API 26) à 14 — à valider
  concrètement sur ton modèle de téléphone exact, les fabricants ayant
  chacun leurs particularités de gestion batterie.
