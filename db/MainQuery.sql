CREATE DATABASE vulnmonitor;
USE vulnmonitor;

CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    UNIQUE (username),
    UNIQUE (email)
);

CREATE TABLE cves (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cve_id VARCHAR(100) NOT NULL,
    description TEXT,
    severity VARCHAR(50),
    affected_product VARCHAR(255) NULL,
    platform VARCHAR(255),
    published_date DATE NULL,
    state VARCHAR(50),
    date_reserved DATE,
    date_updated DATE,
    cvss_score VARCHAR(10),
    cvss_vector VARCHAR(255),
    capec_description TEXT,
    cwe_description TEXT,
    cve_references TEXT,
    affected_versions TEXT,
    credits TEXT,
    UNIQUE (cve_id)
);

CREATE TABLE user_filters (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    os_filter VARCHAR(255),
    severity_filter VARCHAR(50),
    product_filters TEXT,
    include_resolved BOOLEAN NOT NULL,
    include_rejected BOOLEAN NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_alerts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    alerts_json LONGTEXT, -- Stores the alerts as JSON
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_archives (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    cve_id VARCHAR(100) NOT NULL,
    description TEXT,
    severity VARCHAR(50),
    affected_product VARCHAR(255),
    platform VARCHAR(255),
    published_date DATE,
    state VARCHAR(50),
    date_reserved DATE,
    date_updated DATE,
    cvss_score VARCHAR(10),
    cvss_vector VARCHAR(255),
    capec_description TEXT,
    cwe_description TEXT,
    cve_references TEXT,
    affected_versions TEXT,
    credits TEXT,
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_cve (user_id, cve_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (cve_id) REFERENCES cves(cve_id) ON DELETE CASCADE
);

CREATE TABLE user_settings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    system_notifications BOOLEAN NOT NULL,
    sound_alert BOOLEAN NOT NULL,
    last_login TIMESTAMP,
    dark_mode BOOLEAN NOT NULL,
    startup BOOLEAN NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_notifications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    cve_id VARCHAR(100) NOT NULL,
    message TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (cve_id) REFERENCES cves(cve_id) ON DELETE CASCADE
);

CREATE TABLE user_sessions (
    user_id INT PRIMARY KEY,
    session_data LONGTEXT, -- Stores session data as JSON
    session_expiry DATE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE metadata (
    id INT PRIMARY KEY AUTO_INCREMENT,
    key_name VARCHAR(191) UNIQUE NOT NULL,
    dvalue VARCHAR(255) NOT NULL
);