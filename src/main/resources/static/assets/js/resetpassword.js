function ready(fn) {
  if (document.readyState !== 'loading') {
    fn();
    return;
  }
  document.addEventListener('DOMContentLoaded', fn);
}

ready(function () {
    var email = document.getElementById("email");
    var passphrase = document.getElementById("passphrase");
    var passwordError = document.getElementById("passwordError");
    var confirmPasswordError = document.getElementById("confirmPasswordError");
    var submitButton = document.getElementById("submitButton");

    submitButton.addEventListener("click", function (event) {
        event.preventDefault();
        var request = new XMLHttpRequest();
        request.open("POST", "resetpassword/send");
        request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
        request.onreadystatechange = function () {
            if (request.readyState === 4) {
                if (request.status === 200) {
                    window.location.href = "/login";
                } else {
                    alert("Something went wrong. Please try again.");
                }
            }
        };
        var data = JSON.stringify({
            email: email.value,
            passphrase: passphrase.value
        });
        request.send(data);
    });
});
