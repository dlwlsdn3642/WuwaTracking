# WuwaTracking

<p align="center">
  <strong>Bring the official Wuthering Waves launcher’s widget to your phone.</strong>
</p>

<p align="center">
  <a href="./LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-green.svg"></a>
  <img alt="Platform: Android" src="https://img.shields.io/badge/Platform-Android-blue">
  <a href="https://github.com/dlwlsdn3642/WuwaTracking/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/dlwlsdn3642/WuwaTracking?style=social"></a>
  <a href="https://github.com/dlwlsdn3642/WuwaTracking/releases"><img alt="Release" src="https://img.shields.io/github/v/release/dlwlsdn3642/WuwaTracking?display_name=tag"></a>
  <a href="https://github.com/dlwlsdn3642/WuwaTracking/releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/dlwlsdn3642/WuwaTracking/total"></a>
  <a href="../../pulls"><img alt="PRs welcome" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg"></a>
</p>

<p align="center">
  <img src="./.readme_image/app_image_1.png" alt="App screenshot 1" width="45%"/>
  &nbsp;
  <img src="./.readme_image/app_image_2.png" alt="App screenshot 2" width="45%"/>
</p>


## Table of Contents

* [Overview](#overview)
* [Features](#features)
* [Prerequisites](#prerequisites)
* [Quick Start](#quick-start)
* [Configure the App](#configure-the-app)
* [Alarms](#alarms)
* [How it Works](#how-it-works)
* [Troubleshooting](#troubleshooting)
* [FAQ](#faq)
* [Security Notes](#security-notes)
* [License](#license)
* [Disclaimer](#disclaimer)


## Overview

WuwaTracking ports the **widget used by the official Wuthering Waves PC launcher** to Android so you can view key resource info (like Waveplate) on mobile.


## Features

* 📱 **Mobile port of official widget** — No more opening the PC launcher just to check resources.
* ⏰ **Waveplate alarms** — Default at 240 (full), plus **custom thresholds** you define.
* ⚙️ **Lightweight setup** — One-time extraction of `oauthCode` from the PC launcher cache.

> **Note**
> This project is community-made and aims for convenience; verify values against the official launcher if something looks off.


## Prerequisites

You must run the widget **once** inside the **official PC launcher** so it creates the local cache.

<p align="center">
  <img src="./.readme_image/launcher_wiget.png" alt="Launcher widget example" width="70%"/>
</p>


## Quick Start

### 1) Extract your `oauthCode` (Windows / PowerShell)

**Safer (review first):**

```powershell
Invoke-WebRequest -UseBasicParsing `
  -Headers @{ "User-Agent" = "Mozilla/5.0" } `
  -Uri "https://raw.githubusercontent.com/dlwlsdn3642/WuwaTracking/main/KRSDKUsers.ps1" `
  -OutFile "KRSDKUsers.ps1"

# Review the file, then run:
.\KRSDKUsers.ps1
```

**One-liner (convenient, higher risk):**

```powershell
iwr -UseBasicParsing -Headers @{"User-Agent"="Mozilla/5.0"} https://raw.githubusercontent.com/dlwlsdn3642/WuwaTracking/main/KRSDKUsers.ps1 | iex
```

> `KRSDKUsers.ps1` lives in the repo **root**.
> You can open and audit it: [`./KRSDKUsers.ps1`](./KRSDKUsers.ps1)

The script reads:

```
%AppData%\KR_G153\A1730\KRSDKUserLauncherCache.json
```

and prints your decoded **`oauthCode`**, plus **`email`** (or **`thirdNickName`** if email is missing) for each account.


## Configure the App

Install the app → open **Profile settings** → enter:

* **oauthCode** — from the script output
* **UID (playerId)** — your in-game UID
* **Server (region)** — e.g., `Asia`

<p align="center">
  <img src="./.readme_image/app_image_3.png" alt="Profile settings screenshot" width="60%"/>
</p>


## Alarms

By default, the app notifies you when **Waveplate = 240** (full).
Add extra thresholds to be alerted at custom values.

<p align="center">
  <img src="./.readme_image/app_image_4.png" alt="Alarm settings screenshot" width="60%"/>
</p>

> **Important**
> * Disable **battery optimization** for this app.
> * Allow **notification** permission.
>   Without these, alarms may not fire reliably.


## How it Works

1. The official launcher stores an **encrypted** `oauthCode` in:

   ```
   %AppData%\KR_G153\A1730\KRSDKUserLauncherCache.json
   ```
2. The code is decoded via **XOR with 5** (the script handles this).
3. The app communicates with:

   ```
   https://pc-launcher-sdk-api.kurogame.net
   ```

   using a POST body like:

   ```json
   {
     "oauthCode": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
     "playerId": "000000000",
     "region": "Asia"
   }
   ```



## Security Notes

* Treat your **`oauthCode`** like a token. Don’t share it publicly.
* Prefer reviewing scripts before execution.
* This project reads only the specified cache file path and does **not** modify launcher files.


## License

Except for **icon images**, this project is licensed under the **MIT License**.
Icon images have a separate copyright holder and are **excluded** from this project’s license.