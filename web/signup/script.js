document.getElementById('signupForm').addEventListener('submit', function(event) {
    event.preventDefault();

    document.getElementById('usernameError').classList.add('hidden');
    document.getElementById('emailError').classList.add('hidden');
    document.getElementById('passwordError').classList.add('hidden');
    document.getElementById('successMessage').classList.add('hidden');

    let isValid = true;

    const username = sanitizeInput(document.getElementById('username').value);
    if (!username) {
      document.getElementById('usernameError').classList.remove('hidden');
      isValid = false;
    }

    const email = sanitizeInput(document.getElementById('email').value);
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailPattern.test(email)) {
      document.getElementById('emailError').classList.remove('hidden');
      isValid = false;
    }

    const password = sanitizeInput(document.getElementById('password').value);
    if (password.length < 10) {
      document.getElementById('passwordError').classList.remove('hidden');
      isValid = false;
    }

    if (isValid) {
      document.getElementById('successMessage').classList.remove('hidden');
    }
});

function sanitizeInput(input) {
    const element = document.createElement('div');
    element.innerText = input;
    return element.innerHTML;
}