# HaloGuard: IoT Usability vs. Security Trade-Off Research Platform

> **Research Topic:** Evaluating the Usability vs. Security Trade-Off in Consumer IoT  
> **Course / Module:** ITMDA (Information Technology / Multimedia / Data Analytics)  
> **Research Team:** Group 14  
> **Architecture:** Native Android App (Kotlin + Jetpack Compose) + Python Flask REST API + Automated Analytics Pipeline  

---

## 1. Research Overview

Consumer Internet of Things (IoT) devices present a persistent friction between security and usability. By default, commercial off-the-shelf smart home hardware typically prioritizes zero-friction onboarding: devices use unencrypted communication (plaintext MQTT over port 1883), universal default credentials, open UPnP router port forwarding, and unrestricted cloud analytics. While this delivers immediate plug-and-play convenience, it leaves home networks vulnerable to credential stuffing, eavesdropping, and remote botnet exploitation.

Conversely, enforcing strict enterprise-grade security (such as client certificate authentication, network VLAN isolation, and multi-factor biometric step-up) introduces user friction. Non-technical users often struggle with complex configurations, resulting in configuration errors or abandonment.

### Research Problem
How do consumers navigate trade-offs between convenience and security when configuring smart home devices, and does real-time security auditing with transparent risk feedback influence their willingness to adopt hardened configurations?

### Core Research Questions
1. **Trade-Off Distribution:** Given transparent feedback on risk versus convenience, what baseline configuration profiles do consumers choose?
2. **Warning Reversals:** How frequently do users abandon high-convenience / low-security defaults after being presented with concrete vulnerability alerts?
3. **Friction Thresholds:** At what point does increased configuration friction (e.g., certificate distribution or network isolation) prompt users to accept lower security for higher convenience?

---

## 2. The 7 Architectural Trade-Off Settings

The application models 7 critical smart home gateway architectural decisions. Each choice directly impacts the system's **Security Score** (max 100 points) and **Convenience Index** (max 100 points).

| # | Setting | Options | Security (pts) | Usability (pts) | Real-World Impact |
|---|---|---|---|---|---|
| 1 | **Device Authentication** | Default PIN (`0000`)<br>Custom 6-Digit PIN<br>Passphrase + Biometric | 2<br>10<br>15 | 15<br>12<br>6 | Factory PIN leaves gateway open to local brute-force attacks; biometric authentication requires device hardware support. |
| 2 | **Transport Protocol** | MQTT Plain (Port 1883)<br>TLS Encrypted (Port 8883)<br>Strict Mutual TLS (mTLS) | 2<br>16<br>20 | 20<br>16<br>7 | Plaintext transmits telemetry in cleartext; mTLS requires provisioning client and server certificates. |
| 3 | **Network Topology** | Shared Home Wi-Fi<br>Dedicated IoT VLAN | 3<br>15 | 15<br>8 | Shared Wi-Fi exposes personal computers and phones if the hub is compromised; VLAN isolation confines lateral movement. |
| 4 | **Remote Access** | Automatic UPnP<br>Encrypted Cloud Relay<br>Local LAN Only | 2<br>16<br>20 | 20<br>17<br>6 | UPnP opens external ports on the home router; cloud relay eliminates inbound ports; local LAN disables away-from-home control. |
| 5 | **Firmware Policy** | Manual / Disabled<br>Automated Nightly Patch | 2<br>10 | 10<br>7 | Manual updates leave known CVE vulnerabilities unpatched; auto-updates risk service interruption during restarts. |
| 6 | **Data Privacy** | Full Cloud Sharing<br>Anonymized Telemetry<br>Local Only (No Cloud) | 2<br>7<br>10 | 10<br>9<br>5 | Cloud sharing exposes home occupancy patterns to third parties; local only disables cloud voice assistants and external backup. |
| 7 | **Action Authorization** | One-Tap Execution<br>Biometric Step-Up | 2<br>10 | 10<br>6 | One-tap allows quick device switching; biometric step-up confirms user identity before executing climate or lock changes. |

### Scoring & Grading
* **Security Score:** Sum of chosen security points (0 to 100).
* **Convenience Index:** Sum of chosen usability points (0 to 100).
* **Security Grades:**
  * **Grade A (88 - 100 pts):** Hardened Zero-Trust configuration.
  * **Grade B (72 - 87 pts):** Balanced Consumer Protection.
  * **Grade C (50 - 71 pts):** Moderate Exposure Risk.
  * **Grade D (< 50 pts):** Severe Vulnerability Tier (Factory default baseline).

---

## 3. System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│          Android Mobile App (Kotlin + Jetpack Compose)      │
│  - Zero-PII Authentication (Salted SHA-256 Hash)            │
│  - Live Commissioning Auditor (Interactive Gauges & Cards)  │
│  - Real-Time Vulnerability Assessment Engine                │
│  - Active Smart Home Hub (Post-Commissioning Controls)      │
│  - Background Telemetry (Toggle tracker & heartbeats)       │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTPS / JSON REST API
                               ▼
┌─────────────────────────────────────────────────────────────┐
│              Python Flask REST API (app.py)                 │
│  - Bearer Token Authentication                              │
│  - Live Score Calculation & Vulnerability Analysis          │
│  - Commissioning Deployment & Verification                  │
│  - Telemetry Ingestion & Inactivity Detection               │
│  - SQLite Database Storage (database.db)                    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│          Automated Excel Pipeline (export_results.py)       │
│  - Live Excel Generation (results/study_results.xlsx)       │
│  - On-Demand Download Route (GET /api/export/excel)         │
│  - Executive Summary, Participant Audits, Choice Frequency  │
└─────────────────────────────────────────────────────────────┘
```

### Components
1. **Android Client (`android_app/`)**: Built natively with Kotlin and Jetpack Compose. Communicates with the backend using Retrofit and OkHttp. Features dynamic security gauges that update in real time as the user selects configuration options.
2. **Flask Backend (`app.py`, `models.py`)**: Manages participant sessions, validates inputs, logs granular telemetry events, and evaluates configuration profiles against known security standards.
3. **Analytics Pipeline (`export_results.py`)**: Formats collected data into a multi-tab Excel spreadsheet (`study_results.xlsx`) containing descriptive statistics, setting distribution tables, and full interaction logs.

---

## 4. Privacy and Research Ethics

To protect participant privacy and comply with research ethics standards:
* **No Plaintext Identifiers:** Names and raw email addresses are never stored in the database.
* **Salted SHA-256 Hashing:** User accounts use a salted SHA-256 hash of their email address for login and authentication.
* **Participant Pseudonymization:** All exported records reference auto-assigned identifiers (e.g., `P-001`, `P-002`).
* **Minimal Telemetry:** The app logs only configuration adjustments, dwell times, and interface interactions necessary for evaluating usability trade-offs.

---

## 5. Mobile App Features

### 1. Commissioning Auditor Screen
* **Sticky Status Gauges:** Displays the current **Security Score** and **Convenience Index** side by side at the top of the screen, updating instantly with every toggle.
* **Interactive Configuration Cards:** The 7 architectural settings are laid out with selectable chips, contextual descriptions, and points indicators.
* **Real-Time Risk Alerts:** If dangerous settings are selected (such as plaintext MQTT or open UPnP ports), warning banners appear immediately to explain the vulnerability.
* **Diagnostic Scan:** Allows users to run an in-app verification scan before committing changes.

### 2. Post-Commissioning Active Smart Home Hub
Once commissioning is deployed, the app transitions into an operational smart home dashboard:
* Displays live simulated room temperature (`24.8°C`) and environmental readings.
* Interactive light toggle and air conditioning cooling switch.
* Enforces **Biometric Step-Up** on climate modifications if that security policy was enabled during setup.
* Includes a direct button to download the live study report.

---

## 6. Getting Started

### Prerequisites
* Python 3.10 or higher
* Android Studio Iguana / Ladybug or newer
* Android SDK 34 (Android 14) or newer
* Java Development Kit (JDK) 17

### Backend Setup (Local)
1. Open a terminal in the project root directory.
2. Activate your virtual environment:
   ```powershell
   # Windows PowerShell
   .\.venv\Scripts\Activate.ps1
   ```
3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
4. Start the Flask API server:
   ```bash
   python app.py
   ```
   The API will start at `http://127.0.0.1:5000` (or `http://0.0.0.0:5000`).

### Android Setup (Local)
1. Open **Android Studio**.
2. Select **File > Open...** and navigate to the `android_app` directory.
3. Allow Gradle to sync dependencies.
4. Run the application on an Android Emulator or physical device:
   * **Android Emulator:** The app connects to the local backend using `http://10.0.2.2:5000/`.
   * **Physical Device:** Ensure the phone and development PC are on the same local Wi-Fi network. Tap the gear icon on the app login screen and enter your PC's local LAN IP address (e.g., `http://192.168.1.150:5000/`).

---

## 7. Cloud Deployment & Custom Domain

The backend API can be deployed to Render, Railway, or any standard Linux container platform.

### Deploying to Render
1. Push the repository to GitHub.
2. Log in to [Render](https://render.com/) and create a new **Web Service**.
3. Configure the service:
   * **Environment:** Python 3
   * **Build Command:** `pip install -r requirements.txt`
   * **Start Command:** `gunicorn app:app`
4. Add environment variables:
   * `SECRET_KEY`: A secure random string for hashing.

### Custom Subdomain Configuration (`api.rickhomelab.co.za`)
To host the API under your custom domain:
1. In the Render service settings, go to **Settings > Custom Domains**.
2. Add your subdomain: `api.rickhomelab.co.za`.
3. In your DNS provider (e.g., Cloudflare, Namecheap, cPanel), create a **CNAME record**:
   * **Type:** `CNAME`
   * **Host / Name:** `api`
   * **Target / Value:** Your Render service hostname (e.g., `haloguard-api.onrender.com`)
   * **TTL:** Auto or 300 seconds
4. Render will automatically issue and renew a free Let's Encrypt SSL certificate for `https://api.rickhomelab.co.za`.
5. In the mobile app, tap the gear icon on the login screen and set the server URL to:
   ```
   https://api.rickhomelab.co.za/
   ```

---

## 8. Exporting Research Data

The research reporting pipeline generates a structured Excel spreadsheet summarizing study data.

### Method 1: On-Demand Download via API
Open the following endpoint in your web browser:
```
http://localhost:5000/api/export/excel
```
*(Or `https://api.rickhomelab.co.za/api/export/excel` when deployed).*

### Method 2: Manual Script Execution
Run the export script directly from the terminal:
```bash
python export_results.py
```
The report is written to:
```
results/study_results.xlsx
```

### Report Contents
* **Executive_Summary:** Key metrics including total participants, average security score, average usability index, completion rates, and average commissioning duration.
* **Participant_Audits:** Detailed participant records with individual security grades, risk tiers, toggle counts, warning reversals, and timestamps (UTC and SAST).
* **Setting_Choices:** Frequency distributions across all 7 configuration options.
* **Interaction_Logs:** Chronological event logs tracking button clicks, dialog interactions, and session states.
