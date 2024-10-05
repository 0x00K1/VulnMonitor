document.getElementById('signupForm').addEventListener('submit', function(event) {
  event.preventDefault();

  document.getElementById('usernameError').classList.add('hidden');
  document.getElementById('emailError').classList.add('hidden');
  document.getElementById('passwordError').classList.add('hidden');
  document.getElementById('confirmPasswordError').classList.add('hidden');
  document.getElementById('successMessage').classList.add('hidden');

  let isValid = true;

  // Username validation (length between 3 and 20 characters and only letters and numbers)
  const username = document.getElementById('username').value;
  const usernamePattern = /^[a-zA-Z0-9]+$/;
  if (username.length < 3 || username.length > 20 || !usernamePattern.test(username)) {
    document.getElementById('usernameError').innerText = "Username must be between 3 and 20 characters and contain only letters and numbers.";
    document.getElementById('usernameError').classList.remove('hidden');
    isValid = false;
  }

  // Email validation (valid email format and length between 5 and 50 characters)
  const email = document.getElementById('email').value;
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailPattern.test(email) || email.length < 5 || email.length > 50) {
    document.getElementById('emailError').innerText = "Please enter a valid email (5-50 characters).";
    document.getElementById('emailError').classList.remove('hidden');
    isValid = false;
  }

  // Password validation (at least one letter, one digit, one special character, and minimum length of 10)
  const password = document.getElementById('password').value;
  const passwordPattern = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[\W_]).{10,}$/;
  if (!passwordPattern.test(password)) {
    document.getElementById('passwordError').innerText = "Password must be at least 10 characters, include a letter, number, and special character.";
    document.getElementById('passwordError').classList.remove('hidden');
    isValid = false;
  }

  // Confirm password validation
  const confirmPassword = document.getElementById('confirmPassword').value;
  if (isValid && password !== confirmPassword) {
    document.getElementById('confirmPasswordError').innerText = "Passwords do not match.";
    document.getElementById('confirmPasswordError').classList.remove('hidden');
    isValid = false;
  }

  // If all validations pass, submit the form
  if (isValid) {
    document.getElementById('successMessage').classList.remove('hidden');
    document.getElementById('signupForm').submit(); // Submit the form if valid
  }
});