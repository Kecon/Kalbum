package se.kecon.kalbum.auth;

import lombok.Data;

@Data
public class ResetPassword {
    private String email;

    private String passphrase;

    private String password;
}
