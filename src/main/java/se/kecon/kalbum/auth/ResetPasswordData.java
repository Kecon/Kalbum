package se.kecon.kalbum.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;


@AllArgsConstructor
public class ResetPasswordData {

    @Getter
    private final String email;

    @Getter
    private final Instant lastPasswordResetDate;

    @Getter
    private final String passphrase;
}
