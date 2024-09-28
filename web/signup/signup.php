<?php
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    session_start();

    include('db.php');  // Database connection

    function sanitize_input($data) {
        return htmlspecialchars(stripslashes(trim($data)), ENT_QUOTES, 'UTF-8');
    }

    $username = sanitize_input($_POST['username']);
    $email = sanitize_input($_POST['email']);
    $password = sanitize_input($_POST['password']);
    $confirm_password = sanitize_input($_POST['confirmPassword']);

    // Check if the username is between 3 and 20 characters and contains only letters and numbers
    if (strlen($username) < 3 || strlen($username) > 20 || !preg_match('/^[a-zA-Z0-9]+$/', $username)) {
        echo "Username must be between 3 and 20 characters and contain only letters and numbers.";
        exit;
    }

    // Validate email format and check if email length is between 5 and 50 characters
    if (!filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($email) < 5 || strlen($email) > 50) {
        echo "Invalid email format or length (must be between 5 and 50 characters).";
        exit;
    }

    // Check if passwords match
    if ($password !== $confirm_password) {
        echo "Passwords do not match.";
        exit;
    }

    // Password validation: at least one letter, one number, one special character, and minimum length of 10
    if (!preg_match('/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{10,}$/', $password)) {
        echo "Password must be at least 10 characters long and contain at least one uppercase letter, one lowercase letter, and one number.";
        exit;
    }

    // Hash the password securely using bcrypt
    $password_hash = password_hash($password, PASSWORD_BCRYPT);

    // Check if the username or email already exists
    $stmt = $conn->prepare("SELECT id FROM users WHERE username = ? OR email = ?");
    $stmt->bind_param("ss", $username, $email);
    $stmt->execute();
    $stmt->store_result();

    if ($stmt->num_rows > 0) {
        echo "Username or email already exists.";
        exit;
    }

    // Insert new user
    $stmt = $conn->prepare("INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)");
    $stmt->bind_param("sss", $username, $email, $password_hash);

    if ($stmt->execute()) {
        echo "Signup successful!";
    } else {
        // Log the error instead of showing it to the user
        error_log("Error during signup: " . $stmt->error);
        echo "There was an issue during signup. Please try again.";
    }

    $stmt->close();
    $conn->close();
}
?>