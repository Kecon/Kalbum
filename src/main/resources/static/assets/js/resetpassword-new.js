function ready(fn) {
  if (document.readyState !== 'loading') {
    fn();
    return;
  }
  document.addEventListener('DOMContentLoaded', fn);
}

ready(function () {
    var password = document.getElementById("password");
    var confirmPassword = document.getElementById("confirmPassword");
    var passphrase = document.getElementById("passphrase");
    var passwordError = document.getElementById("passwordError");
    var confirmPasswordError = document.getElementById("confirmPasswordError");
    var submitButton = document.getElementById("submitButton");

    submitButton.disabled = true;

    password.addEventListener("input", function (event) {
        if (password.value.length < 8) {
            passwordError.innerHTML = "Password must be at least 8 characters long.";
            passwordValid = false;
        } else {
            passwordError.innerHTML = "";
            passwordValid = true;
        }
        if (password.value !== confirmPassword.value) {
            confirmPasswordError.innerHTML = "Passwords do not match.";
            confirmPasswordValid = false;
        } else {
            confirmPasswordError.innerHTML = "";
            confirmPasswordValid = true;
        }
        if (passwordValid && confirmPasswordValid) {
            submitButton.disabled = false;
        } else {
            submitButton.disabled = true;
        }
    });

    confirmPassword.addEventListener("input", function (event) {
        if (password.value !== confirmPassword.value) {
            confirmPasswordError.innerHTML = "Passwords do not match.";
            confirmPasswordValid = false;
        } else {
            confirmPasswordError.innerHTML = "";
            confirmPasswordValid = true;
        }
        if (passwordValid && confirmPasswordValid) {
            submitButton.disabled = false;
        } else {
            submitButton.disabled = true;
        }
    });

    submitButton.addEventListener("click", function (event) {
        event.preventDefault();
        var request = new XMLHttpRequest();
        request.open("PUT", "/resetpassword/" + document.getElementById("token").value);
        request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
        request.onreadystatechange = function () {
            if (request.readyState === 4) {
                if (request.status === 204) {
                    window.location.href = "/login";
                } else {
                    alert("Something went wrong. Please try again.");
                }
            }
        };
        var data = JSON.stringify({
            password: password.value,
            passphrase: passphrase.value
        });
        request.send(data);
    });
});
