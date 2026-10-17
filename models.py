"""
Database models for the IoT usability research platform.
Defines schemas for participants, gateway commissioning sessions, and interaction telemetry.
"""

from datetime import datetime, timezone
import secrets
from flask_sqlalchemy import SQLAlchemy
from werkzeug.security import generate_password_hash, check_password_hash

db = SQLAlchemy()


def utc_now():
    """Returns current UTC timestamp with timezone awareness."""
    return datetime.now(timezone.utc)


class User(db.Model):
    """
    Participant account model.
    To protect participant privacy, email addresses are stored as salted SHA-256 hashes.
    No plaintext names or emails are stored in the database.
    """
    __tablename__ = "users"

    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    email_hash = db.Column(db.String(64), unique=True, index=True, nullable=False)
    password_hash = db.Column(db.String(255), nullable=False)
    auth_token = db.Column(db.String(64), unique=True, index=True, nullable=True)
    created_at = db.Column(db.DateTime(timezone=True), default=utc_now, nullable=False)

    # 1:1 relationship with commissioning session
    commission_session = db.relationship(
        "CommissionSession",
        back_populates="user",
        uselist=False,
        cascade="all, delete-orphan",
    )
    
    # 1:N relationship with telemetry interaction logs
    interaction_logs = db.relationship(
        "InteractionLog",
        back_populates="user",
        cascade="all, delete-orphan",
        order_by="InteractionLog.timestamp.asc()",
    )

    def set_password(self, password: str):
        """Generates secure scrypt password hash."""
        self.password_hash = generate_password_hash(password)

    def check_password(self, password: str) -> bool:
        """Verifies password against stored hash."""
        if not self.password_hash:
            return False
        return check_password_hash(self.password_hash, password)

    def generate_auth_token(self) -> str:
        """Creates a cryptographically secure 64-character bearer token."""
        self.auth_token = secrets.token_hex(32)
        return self.auth_token

    def to_dict(self):
        """Serializes user record with pseudonymized participant ID."""
        return {
            "id": self.id,
            "participant_id": f"P-{self.id:03d}",
            "email_hash": self.email_hash,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }


class CommissionSession(db.Model):
    """
    Tracks the gateway configuration session for each participant.
    Records the 7 chosen security/usability settings, final scores, and interaction counts.
    """
    __tablename__ = "commission_sessions"

    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    user_id = db.Column(
        db.Integer,
        db.ForeignKey("users.id", ondelete="CASCADE"),
        unique=True,
        nullable=False,
    )
    
    # Session timing and completion status
    started_at = db.Column(db.DateTime(timezone=True), nullable=True)
    completed_at = db.Column(db.DateTime(timezone=True), nullable=True)
    last_active_at = db.Column(db.DateTime(timezone=True), default=utc_now, nullable=False)
    total_time_seconds = db.Column(db.Float, nullable=True)
    status = db.Column(db.String(20), default="in_progress", nullable=False)  # in_progress, commissioned, abandoned

    # The 7 IoT Architectural Trade-off Settings
    auth_credential = db.Column(db.String(50), default="default_pin", nullable=False)
    transport_crypto = db.Column(db.String(50), default="mqtt_plain_1883", nullable=False)
    network_topology = db.Column(db.String(50), default="shared_home_wifi", nullable=False)
    remote_exposure = db.Column(db.String(50), default="upnp_port", nullable=False)
    firmware_policy = db.Column(db.String(50), default="manual_never", nullable=False)
    data_privacy = db.Column(db.String(50), default="full_cloud_sharing", nullable=False)
    action_auth = db.Column(db.String(50), default="one_tap_always", nullable=False)

    # Computed audit scores based on selected settings
    final_security_score = db.Column(db.Float, default=20.0, nullable=False)
    final_usability_score = db.Column(db.Float, default=97.0, nullable=False)
    security_grade = db.Column(db.String(5), default="D-", nullable=False)
    risk_tier = db.Column(db.String(50), default="High Vulnerability", nullable=False)

    # Participant interaction metrics
    toggle_count = db.Column(db.Integer, default=0, nullable=False)
    warning_reversals = db.Column(db.Integer, default=0, nullable=False)

    user = db.relationship("User", back_populates="commission_session")

    def to_dict(self):
        """Serializes session state for API responses."""
        return {
            "id": self.id,
            "user_id": self.user_id,
            "status": self.status,
            "started_at": self.started_at.isoformat() if self.started_at else None,
            "completed_at": self.completed_at.isoformat() if self.completed_at else None,
            "total_time_seconds": self.total_time_seconds,
            "settings": {
                "auth_credential": self.auth_credential,
                "transport_crypto": self.transport_crypto,
                "network_topology": self.network_topology,
                "remote_exposure": self.remote_exposure,
                "firmware_policy": self.firmware_policy,
                "data_privacy": self.data_privacy,
                "action_auth": self.action_auth,
            },
            "scores": {
                "security_score": self.final_security_score,
                "usability_score": self.final_usability_score,
                "grade": self.security_grade,
                "risk_tier": self.risk_tier,
            },
            "metrics": {
                "toggle_count": self.toggle_count,
                "warning_reversals": self.warning_reversals,
            },
        }


class InteractionLog(db.Model):
    """
    Append-only telemetry event log.
    Captures user interactions, setting adjustments, warnings displayed, and authentication events.
    """
    __tablename__ = "interaction_logs"

    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    user_id = db.Column(
        db.Integer,
        db.ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
        nullable=False,
    )
    event_type = db.Column(db.String(50), index=True, nullable=False)
    event_details = db.Column(db.JSON, nullable=True)
    timestamp = db.Column(db.DateTime(timezone=True), default=utc_now, nullable=False)

    user = db.relationship("User", back_populates="interaction_logs")

    def to_dict(self):
        """Serializes log entry for reporting."""
        return {
            "id": self.id,
            "user_id": self.user_id,
            "event_type": self.event_type,
            "event_details": self.event_details,
            "timestamp": self.timestamp.isoformat() if self.timestamp else None,
        }
