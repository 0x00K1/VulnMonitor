document.getElementById('signupForm').addEventListener('submit', function(event) {
  event.preventDefault();
  document.getElementById('usernameError').classList.add('hidden');
  document.getElementById('emailError').classList.add('hidden');
  document.getElementById('passwordError').classList.add('hidden');
  document.getElementById('confirmPasswordError').classList.add('hidden');
  document.getElementById('successMessage').classList.add('hidden');

  let isValid = true;

  const username = document.getElementById('username').value;
  if (!username) {
    document.getElementById('usernameError').classList.remove('hidden');
    isValid = false;
  }

  const email = document.getElementById('email').value;
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailPattern.test(email)) {
    document.getElementById('emailError').classList.remove('hidden');
    isValid = false;
  }

  const password = document.getElementById('password').value;
  if (password.length < 10) {
    document.getElementById('passwordError').classList.remove('hidden');
    isValid = false;
  }

  const confirmPassword = document.getElementById('confirmPassword').value;
  if (password !== confirmPassword) {
    document.getElementById('confirmPasswordError').classList.remove('hidden');
    isValid = false;
  }

  if (isValid) {
    document.getElementById('successMessage').classList.remove('hidden');
    document.getElementById('signupForm').submit(); // Submit the form if valid
  }
});