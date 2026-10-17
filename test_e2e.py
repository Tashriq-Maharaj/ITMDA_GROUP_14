import json
import os
from app import app, db
from models import User, CommissionSession

def run_e2e_tests():
    client = app.test_client()

    print("--- 1. Testing root endpoint ---")
    res = client.get("/")
    assert res.status_code == 200, f"Root endpoint failed: {res.status_code}"
    print("Root endpoint: OK")

    print("--- 2. Testing user registration ---")
    reg_payload = {"email": "participant01@testlab.co.za", "password": "SafePassword123!"}
    res = client.post("/api/auth/register", json=reg_payload)
    assert res.status_code in (200, 201), f"Register failed: {res.data}"
    reg_data = json.loads(res.data)
    token = reg_data["token"]
    assert token, "Token missing in response"
    print(f"Register OK: User ID {reg_data['user']['id']}, Participant {reg_data['user']['participant_id']}")

    print("--- 3. Testing duplicate registration / login ---")
    res = client.post("/api/auth/login", json=reg_payload)
    assert res.status_code == 200, f"Login failed: {res.data}"
    login_data = json.loads(res.data)
    assert login_data["token"], "Login token missing"
    token = login_data["token"]
    print("Login OK")

    print("--- 4. Testing real-time security auditor endpoint ---")
    headers = {"Authorization": f"Bearer {token}"}
    settings_payload = {
        "auth_credential": "pin_6",
        "transport_crypto": "tls_8883",
        "network_topology": "isolated_vlan",
        "remote_exposure": "cloud_relay",
        "firmware_policy": "auto_nightly_patch",
        "data_privacy": "anonymized_telemetry",
        "action_auth": "biometric_step_up",
    }
    res = client.post("/api/commission/audit", headers=headers, json=settings_payload)
    assert res.status_code == 200, f"Audit failed: {res.data}"
    audit_data = json.loads(res.data)["audit"]
    print(f"Audit OK: Security Score: {audit_data['security_score']}, Usability Score: {audit_data['usability_score']}, Grade: {audit_data['security_grade']}, Tier: {audit_data['risk_tier']}")

    print("--- 5. Testing telemetry logging ---")
    res = client.post(
        "/api/telemetry/event",
        headers=headers,
        json={"event_type": "setting_toggled", "event_details": {"setting": "transport_crypto", "value": "tls_8883"}},
    )
    assert res.status_code == 200, f"Telemetry event failed: {res.data}"

    res = client.post("/api/telemetry/heartbeat", headers=headers)
    assert res.status_code == 200, f"Heartbeat failed: {res.data}"
    print("Telemetry & Heartbeat OK")

    print("--- 6. Testing commissioning deployment ---")
    deploy_payload = {
        "settings": settings_payload,
        "toggle_count": 12,
        "warning_reversals": 2,
    }
    res = client.post("/api/commission/deploy", headers=headers, json=deploy_payload)
    assert res.status_code == 200, f"Deploy failed: {res.data}"
    deploy_res = json.loads(res.data)
    assert deploy_res["success"] is True
    session_data = deploy_res["session"]
    assert session_data["status"] == "commissioned"
    scores = session_data["scores"]
    print(f"Deploy OK: Status={session_data['status']}, Security={scores['security_score']}, Usability={scores['usability_score']}, Grade={scores['grade']}")

    print("--- 7. Testing user profile endpoint after deployment ---")
    res = client.get("/api/user/profile", headers=headers)
    assert res.status_code == 200, f"Profile failed: {res.data}"
    profile_data = json.loads(res.data)
    assert profile_data["session"]["status"] == "commissioned"
    print("User profile OK")

    print("--- 8. Testing Excel live export endpoint ---")
    res = client.get("/api/export/excel")
    assert res.status_code == 200, f"Excel export failed: {res.status_code}"
    assert len(res.data) > 1000, f"Excel export payload suspiciously small: {len(res.data)} bytes"
    print(f"Excel export OK: Received {len(res.data)} bytes")

    print("--- 9. Testing raw database.db export endpoint ---")
    res = client.get("/api/export/database")
    assert res.status_code == 200, f"Database export failed: {res.status_code}"
    assert len(res.data) > 1000, f"Database file payload suspiciously small: {len(res.data)} bytes"
    print(f"Database export OK: Received {len(res.data)} bytes")

    print("--- 10. Testing logout endpoint ---")
    res = client.post("/api/auth/logout", headers=headers)
    assert res.status_code == 200, f"Logout failed: {res.data}"
    logout_data = json.loads(res.data)
    assert logout_data["success"] is True
    print("Logout OK")

    print("\nALL END-TO-END TESTS COMPLETED SUCCESSFULLY!")

if __name__ == "__main__":
    run_e2e_tests()
