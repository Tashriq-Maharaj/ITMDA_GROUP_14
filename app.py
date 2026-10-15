import os
import hashlib
from datetime import datetime, timezone, timedelta
from flask import Flask, request, jsonify, send_file
from models import db, User, CommissionSession, InteractionLog, utc_now

app = Flask(__name__)
app.secret_key = os.environ.get("SECRET_KEY", "iot-security-usability-secret-2026")

BASE_DIR = os.path.abspath(os.path.dirname(__file__))
DB_PATH = os.path.join(BASE_DIR, "database.db")
app.config["SQLALCHEMY_DATABASE_URI"] = f"sqlite:///{DB_PATH}"
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

db.init_app(app)

with app.app_context():
    db.create_all()

ABANDONMENT_TIMEOUT_MINUTES = 2


# ---------------------------------------------------------------------------
# CORS HEADERS
# ---------------------------------------------------------------------------
@app.after_request
def add_cors_headers(response):
    response.headers["Access-Control-Allow-Origin"] = "*"
    response.headers["Access-Control-Allow-Headers"] = "Content-Type,Authorization"
    response.headers["Access-Control-Allow-Methods"] = "GET,POST,PUT,DELETE,OPTIONS"
    return response


@app.route("/api/<path:dummy>", methods=["OPTIONS"])
def api_options(dummy):
    return "", 204


def hash_identifier(identifier: str) -> str:
    normalized = identifier.strip().lower()
    salt = app.secret_key.encode("utf-8")
    return hashlib.sha256(salt + normalized.encode("utf-8")).hexdigest()


def ensure_utc(dt):
    if dt is None:
        return None
    if dt.tzinfo is None:
        return dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc)


def sweep_abandoned_sessions(timeout_minutes=ABANDONMENT_TIMEOUT_MINUTES):
    now = utc_now()
    threshold = now - timedelta(minutes=timeout_minutes)

    stale = CommissionSession.query.filter(
        CommissionSession.status == "in_progress",
        CommissionSession.last_active_at < threshold,
    ).all()

    for sess in stale:
        sess.status = "abandoned"
        start_utc = ensure_utc(sess.started_at)
        last_utc = ensure_utc(sess.last_active_at)
        if start_utc and last_utc:
            sess.total_time_seconds = max(0.1, (last_utc - start_utc).total_seconds())

        log = InteractionLog(
            user_id=sess.user_id,
            event_type="session_abandoned",
            event_details={
                "reason": "inactivity_timeout",
                "timeout_minutes": timeout_minutes,
                "time_spent_seconds": sess.total_time_seconds,
            },
            timestamp=now,
        )
        db.session.add(log)

    if stale:
        db.session.commit()
    return len(stale)


def get_current_user():
    auth_header = request.headers.get("Authorization", "").strip()
    if auth_header.startswith("Bearer "):
        token = auth_header.split(" ", 1)[1].strip()
        user = User.query.filter_by(auth_token=token).first()
        if user:
            return user, user.commission_session
    return None, None


# ---------------------------------------------------------------------------
# SCORING ENGINE
# ---------------------------------------------------------------------------
WEIGHTS = {
    "auth_credential": {
        "default_pin": (2, 15),
        "pin_6": (10, 12),
        "passphrase_biometric": (15, 6),
    },
    "transport_crypto": {
        "mqtt_plain_1883": (2, 20),
        "tls_8883": (16, 16),
        "mtls_strict": (20, 7),
    },
    "network_topology": {
        "shared_home_wifi": (3, 15),
        "isolated_vlan": (15, 8),
    },
    "remote_exposure": {
        "upnp_port": (2, 20),
        "cloud_relay": (16, 17),
        "local_lan": (20, 6),
    },
    "firmware_policy": {
        "manual_never": (2, 10),
        "auto_nightly_patch": (10, 7),
    },
    "data_privacy": {
        "full_cloud_sharing": (2, 10),
        "anonymized_telemetry": (7, 9),
        "local_only": (10, 5),
    },
    "action_auth": {
        "one_tap_always": (2, 10),
        "biometric_step_up": (10, 6),
    },
}


def calculate_scores(settings: dict):
    sec_total = 0.0
    use_total = 0.0

    for key, choices in WEIGHTS.items():
        val = settings.get(key)
        if val in choices:
            s, u = choices[val]
        else:
            default_val = list(choices.keys())[0]
            s, u = choices[default_val]
        sec_total += s
        use_total += u

    # Determine security grade
    if sec_total >= 88:
        grade = "A"
        tier = "Hardened Zero-Trust"
    elif sec_total >= 72:
        grade = "B"
        tier = "Balanced Consumer Protection"
    elif sec_total >= 50:
        grade = "C"
        tier = "Moderate Exposure Risk"
    else:
        grade = "D"
        tier = "High Vulnerability"

    return round(sec_total, 1), round(use_total, 1), grade, tier


def run_vulnerability_assessment(settings: dict):
    risks = []
    strengths = []

    if settings.get("auth_credential") == "default_pin":
        risks.append({"name": "Weak Authentication", "desc": "Default 4-digit PIN vulnerable to automated dictionary brute-forcing."})
    else:
        strengths.append("Robust credential authentication active.")

    if settings.get("transport_crypto") == "mqtt_plain_1883":
        risks.append({"name": "Plaintext Transmission", "desc": "MQTT port 1883 transmits telemetry and credentials unencrypted over local network."})
    else:
        strengths.append("Transport encryption (TLS/mTLS) protects data in transit.")

    if settings.get("network_topology") == "shared_home_wifi":
        risks.append({"name": "No Subnet Isolation", "desc": "Hub shares network with personal computers, allowing lateral attack movement."})
    else:
        strengths.append("Device isolated on dedicated IoT VLAN subnet.")

    if settings.get("remote_exposure") == "upnp_port":
        risks.append({"name": "Public Port Exposure", "desc": "UPnP opened inbound router port, discoverable by public internet scanners (Shodan)."})
    elif settings.get("remote_exposure") == "cloud_relay":
        strengths.append("Protected behind authenticated cloud relay broker.")
    else:
        strengths.append("Local-only mode: zero external internet attack surface.")

    if settings.get("firmware_policy") == "manual_never":
        risks.append({"name": "Unpatched Vulnerabilities", "desc": "Automated security patches disabled; vulnerable to known CVE exploits."})
    else:
        strengths.append("Automated nightly patch management enabled.")

    if settings.get("data_privacy") == "full_cloud_sharing":
        risks.append({"name": "Data Sovereignty Risk", "desc": "Full household occupancy and telemetry shared with third-party cloud analytics."})
    else:
        strengths.append("Data privacy controls protect user household telemetry.")

    if settings.get("action_auth") == "one_tap_always":
        risks.append({"name": "Physical Access Risk", "desc": "Anyone holding an unlocked phone can change security and climate rules."})
    else:
        strengths.append("Biometric step-up authentication required for critical actions.")

    return risks, strengths


# ---------------------------------------------------------------------------
# API ENDPOINTS
# ---------------------------------------------------------------------------
@app.route("/")
def index():
    return jsonify({
        "service": "HaloGuard IoT Commissioning & Security Auditor API",
        "version": "2.0.0",
        "status": "online",
        "documentation": "Evaluating the Usability vs. Security Tradeoff in Consumer IoT"
    })


@app.route("/api/auth/register", methods=["POST"])
def api_register():
    sweep_abandoned_sessions()
    data = request.get_json(silent=True) or {}
    email = data.get("email", "").strip().lower()
    password = data.get("password", "").strip()

    if not email or "@" not in email:
        return jsonify({"success": False, "error": "A valid email address is required."}), 400
    if not password or len(password) < 4:
        return jsonify({"success": False, "error": "Password must be at least 4 characters."}), 400

    email_hash = hash_identifier(email)
    existing_user = User.query.filter_by(email_hash=email_hash).first()

    if existing_user:
        if not existing_user.check_password(password):
            return jsonify({"success": False, "error": "Account exists with a different password."}), 409

        token = existing_user.generate_auth_token()
        db.session.commit()
        return jsonify({
            "success": True,
            "token": token,
            "user": existing_user.to_dict(),
            "session": existing_user.commission_session.to_dict() if existing_user.commission_session else None,
        })

    new_user = User(
        email_hash=email_hash,
        created_at=utc_now(),
    )
    new_user.set_password(password)
    token = new_user.generate_auth_token()
    db.session.add(new_user)
    db.session.flush()

    new_session = CommissionSession(
        user_id=new_user.id,
        started_at=utc_now(),
        last_active_at=utc_now(),
        status="in_progress",
    )
    db.session.add(new_session)

    log = InteractionLog(
        user_id=new_user.id,
        event_type="user_registered",
        event_details={"participant_hash": email_hash[:12]},
        timestamp=utc_now(),
    )
    db.session.add(log)
    db.session.commit()

    return jsonify({
        "success": True,
        "token": token,
        "user": new_user.to_dict(),
        "session": new_session.to_dict(),
    }), 201


@app.route("/api/auth/login", methods=["POST"])
def api_login():
    sweep_abandoned_sessions()
    data = request.get_json(silent=True) or {}
    email = data.get("email", "").strip().lower()
    password = data.get("password", "").strip()

    if not email or not password:
        return jsonify({"success": False, "error": "Email and password required."}), 400

    email_hash = hash_identifier(email)
    user = User.query.filter_by(email_hash=email_hash).first()

    if not user or not user.check_password(password):
        return jsonify({"success": False, "error": "Invalid email or password."}), 401

    token = user.generate_auth_token()
    db.session.commit()

    return jsonify({
        "success": True,
        "token": token,
        "user": user.to_dict(),
        "session": user.commission_session.to_dict() if user.commission_session else None,
    })


@app.route("/api/user/profile", methods=["GET"])
def api_profile():
    user, sess = get_current_user()
    if not user:
        return jsonify({"success": False, "error": "Unauthorized"}), 401

    return jsonify({
        "success": True,
        "user": user.to_dict(),
        "session": sess.to_dict() if sess else None,
    })


@app.route("/api/commission/calculate", methods=["POST"])
@app.route("/api/commission/audit", methods=["POST"])
def api_calculate():
    """Live preview of scores for given settings without saving."""
    data = request.get_json(silent=True) or {}
    settings = data.get("settings", data)
    sec_score, use_score, grade, tier = calculate_scores(settings)
    risks, strengths = run_vulnerability_assessment(settings)

    return jsonify({
        "security_score": sec_score,
        "usability_score": use_score,
        "grade": grade,
        "risk_tier": tier,
        "risks": risks,
        "strengths": strengths,
        "audit": {
            "security_score": sec_score,
            "usability_score": use_score,
            "security_grade": grade,
            "risk_tier": tier,
            "risks": risks,
            "strengths": strengths,
        }
    })


@app.route("/api/commission/deploy", methods=["POST"])
def api_deploy():
    """Finalizes commissioning, records metrics, and returns the audit certificate."""
    user, sess = get_current_user()
    if not user or not sess:
        return jsonify({"success": False, "error": "Unauthorized"}), 401

    data = request.get_json(silent=True) or {}
    settings = data.get("settings", {})
    metrics = data.get("metrics", {})

    now = utc_now()
    sec_score, use_score, grade, tier = calculate_scores(settings)
    risks, strengths = run_vulnerability_assessment(settings)

    # Save settings to session
    sess.auth_credential = settings.get("auth_credential", "default_pin")
    sess.transport_crypto = settings.get("transport_crypto", "mqtt_plain_1883")
    sess.network_topology = settings.get("network_topology", "shared_home_wifi")
    sess.remote_exposure = settings.get("remote_exposure", "upnp_port")
    sess.firmware_policy = settings.get("firmware_policy", "manual_never")
    sess.data_privacy = settings.get("data_privacy", "full_cloud_sharing")
    sess.action_auth = settings.get("action_auth", "one_tap_always")

    sess.final_security_score = sec_score
    sess.final_usability_score = use_score
    sess.security_grade = grade
    sess.risk_tier = tier

    sess.toggle_count = int(metrics.get("toggle_count", data.get("toggle_count", 0)))
    sess.warning_reversals = int(metrics.get("warning_reversals", data.get("warning_reversals", 0)))

    sess.completed_at = now
    sess.last_active_at = now
    sess.status = "commissioned"

    if sess.started_at:
        delta = now - ensure_utc(sess.started_at)
        sess.total_time_seconds = max(0.1, delta.total_seconds())
    else:
        sess.total_time_seconds = 0.0

    log = InteractionLog(
        user_id=user.id,
        event_type="gateway_commissioned",
        event_details={
            "settings": settings,
            "security_score": sec_score,
            "usability_score": use_score,
            "grade": grade,
            "risk_tier": tier,
            "total_time_seconds": sess.total_time_seconds,
            "toggle_count": sess.toggle_count,
            "warning_reversals": sess.warning_reversals,
        },
        timestamp=now,
    )
    db.session.add(log)
    db.session.commit()

    # Trigger background Excel update
    try:
        from export_results import export_database_to_excel
        export_database_to_excel()
    except Exception as e:
        print(f"Background export note: {e}")

    return jsonify({
        "success": True,
        "message": "Gateway successfully commissioned.",
        "session": sess.to_dict(),
        "audit_report": {
            "participant_id": f"P-{user.id:03d}",
            "security_score": sec_score,
            "usability_score": use_score,
            "grade": grade,
            "risk_tier": tier,
            "total_time_seconds": sess.total_time_seconds,
            "toggle_count": sess.toggle_count,
            "warning_reversals": sess.warning_reversals,
            "risks": risks,
            "strengths": strengths,
        }
    })


@app.route("/api/telemetry/event", methods=["POST"])
def api_telemetry_event():
    user, sess = get_current_user()
    if not user or not sess:
        return jsonify({"status": "unauthorized"}), 401

    data = request.get_json(silent=True) or {}
    event_type = data.get("event_type", "interaction")
    event_details = data.get("event_details", {})

    now = utc_now()
    sess.last_active_at = now

    if event_type == "app_closed_or_backgrounded" and sess.status == "in_progress":
        sess.status = "abandoned"
        start_utc = ensure_utc(sess.started_at)
        if start_utc:
            sess.total_time_seconds = max(0.1, (now - start_utc).total_seconds())

    log = InteractionLog(
        user_id=user.id,
        event_type=str(event_type)[:50],
        event_details=event_details,
        timestamp=now,
    )
    db.session.add(log)
    db.session.commit()

    return jsonify({"status": "logged"})


@app.route("/api/telemetry/heartbeat", methods=["POST"])
def api_heartbeat():
    user, sess = get_current_user()
    if not user or not sess:
        return jsonify({"status": "unauthorized"}), 401

    now = utc_now()
    sess.last_active_at = now
    db.session.commit()
    return jsonify({"status": "ok", "last_active_at": now.isoformat()})


@app.route("/api/telemetry/abandon", methods=["POST"])
def api_abandon():
    user, sess = get_current_user()
    if not user or not sess:
        return jsonify({"status": "unauthorized"}), 401

    now = utc_now()
    data = request.get_json(silent=True) or {}
    reason = data.get("reason", "voluntary_exit")

    if sess.status == "in_progress":
        sess.status = "abandoned"
        sess.last_active_at = now
        start_utc = ensure_utc(sess.started_at)
        if start_utc:
            sess.total_time_seconds = max(0.1, (now - start_utc).total_seconds())

        log = InteractionLog(
            user_id=user.id,
            event_type="session_abandoned",
            event_details={
                "reason": reason,
                "time_spent_seconds": sess.total_time_seconds,
            },
            timestamp=now,
        )
        db.session.add(log)
        db.session.commit()

    return jsonify({"status": "abandoned"})


# ---------------------------------------------------------------------------
# ON-DEMAND LIVE EXCEL REPORT DOWNLOAD ENDPOINT
# ---------------------------------------------------------------------------
@app.route("/api/export/excel", methods=["GET"])
def api_export_excel():
    """Generates the latest Excel report on the fly and serves it as a download."""
    try:
        from export_results import export_database_to_excel, DEFAULT_EXCEL_OUTPUT
        file_path = export_database_to_excel()
        return send_file(
            file_path,
            as_attachment=True,
            download_name="IoT_Usability_Security_Study_Results.xlsx",
            mimetype="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    except Exception as e:
        return jsonify({"error": f"Report generation failed: {str(e)}"}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
