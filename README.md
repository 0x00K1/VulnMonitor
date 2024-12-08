<p align="center">
  <img src="repo/ICON.png" alt="Icon">
</p>

<h1 align="center"># VulnMonitor  |  Demo</h1>

VulnMonitor is a robust desktop application designed to streamline the monitoring of Common Vulnerabilities and Exposures (CVEs). This tool helps users stay informed about security threats relevant to their software environment by providing real-time tracking, customizable filters, and alert notifications.




## ⭐ Features

- **📡 Real-Time CVE Monitoring**: Fetches the latest CVEs every 15 minutes (adjustable).
- **🔍 Customizable Filters**: Filter CVEs by severity, affected product, or other criteria.
- **📨 Personalized Alerts**: Get notified through system alerts or emails.
- **🗂️ Data Archiving**: Save important CVEs for future reference.
- **⚙️ User Preferences**: Dark mode, customizable settings, and persistent preferences.
- **🖥️ Background Operation**: Runs silently in the background and starts with the system.
- **🔐 Secure Storage**: Stores user data and preferences securely in a MySQL database.




## 🛠️ Technologies Used

```bash
1. Programming Language: Java (for backend and GUI development)
2. UI Framework: Swing
3. Database: MySQL
4. API Integration: RESTful APIs for fetching CVE data (e.g., NVD, MITRE)
5. Email Service: JavaMail API with MailerSend
6. Web Components: HTML, CSS, JS, PHP
```




## 📥 Installation

### Prerequisites

```bash
1. Java JDK (version 17 or higher)
2. MySQL Server
3. PHP and Apache (or a similar web server)
4. Internet connection to fetch CVE data
5. A code editor or IDE (e.g., VSCode, Eclipse)
6. MailerSend account for email configuration
```

### Steps

1. Clone the repository:

```bash
git clone https://github.com/0x00K1/VulnMonitor.git
cd VulnMonitor
```

2. Configure the database for the web components:

   - Open the `web/db.php` file and update the database credentials:

     ```php
     <?php
     $servername = "localhost";
     $username = "your_username";
     $password = "your_password";
     $dbname = "vulnmonitor";
     ?>
     ```

3. Configure the database for the application:

   - Create a MySQL database named `vulnmonitor`:

   ```bash
   mysql -u root -p -e "CREATE DATABASE vulnmonitor;"
   ```

   - Import the provided SQL schema file:

   ```bash
   mysql -u root -p vulnmonitor < MainQuery.sql
   ```

4. Update database credentials in the application configuration:

   ```java
   // File: src/com/vulnmonitor/services/DatabaseService.java
   public static final String DB_URL = "jdbc:mysql://localhost:3306/vulnmonitor";
   public static final String DB_USER = "your_user";
   public static final String DB_PASSWORD = "your_password";
   ```

5. Update your API key for CVE fetching:

   ```java
   // File: src/com/vulnmonitor/utils/APIUtils.java
   private static final String API_KEY = "your-api-key-here";
   ```

6. Configure the email service:

   ```java
   // File: src/com/vulnmonitor/services/EmailService.java
   private final String smtpHost = "smtp.mailersend.net";
   private final String smtpPort = "587";
   private final String username = "your_mailersend_username";
   private final String password = "your_mailersend_password";
   ```

7. Build and run the application:

```bash
javac -cp . com/vulnmonitor/Main.java
java -cp . com.vulnmonitor.Main
```




## 🚀 Usage

```bash
1. Launch the application.
2. Log in or create an account.
3. Configure filters based on your requirements (e.g., OS, severity).
4. Monitor real-time CVEs via the dashboard.
5. Set up alerts for critical vulnerabilities.
6. Use the settings menu to customize preferences such as appearance or refresh intervals.
```




## 📂 Project Structure

```plaintext
src/
├── com.vulnmonitor/
│   ├── gui/               # Graphical user interface components
│   ├── model/             # Data models
│   ├── services/          # Services (e.g., API, database, email)
│   ├── utils/             # Utility classes
│   └── Main.java          # Entry point of the application
db/
└── MainQuery.sql          # Database schema
```




## 🖥️ System Architecture
<p align="center">
  <img src="repo/SysArch.svg" alt="System Architecture">
</p>
