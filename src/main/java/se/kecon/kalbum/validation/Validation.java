/**
 * Kalbum
 * <p>
 * Copyright 2023 Kenny Colliander
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.kecon.kalbum.validation;

import se.kecon.kalbum.auth.Role;

import java.util.regex.Pattern;

/**
 * Validation methods for album id and content filename.
 *
 * @author Kenny Colliander
 * @since 2023-08-03
 */
public class Validation {

    protected static final Pattern VALID_ALBUM_ID = Pattern.compile("^[_a-zA-Z0-9\\-]+$");

    protected static final Pattern VALID_FILENAME = Pattern.compile("^[^\\\\/:*?\"<>|]+\\.(?i)(png|jpe?g|mp4)$");

    protected static final Pattern VALID_USERNAME = Pattern.compile("^[_a-zA-Z0-9\\-.]+$");

    protected static final Pattern VALID_EMAIL = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    /**
     * Hide constructor
     */
    private Validation() {
    }

    /**
     * Check if the id is valid and not contain any invalid characters
     *
     * @param id album id
     * @throws IllegalAlbumIdException if the id is invalid
     */
    public static void checkValidAlbumId(final String id) throws IllegalAlbumIdException {
        if (!VALID_ALBUM_ID.matcher(id).matches()) {
            throw new IllegalAlbumIdException("Invalid album id: " + id);
        }
    }

    /**
     * Check if the filename is valid and not contain any invalid characters
     *
     * @param filename content filename
     * @throws IllegalFilenameException if the filename is invalid
     */
    public static void checkValidFilename(final String filename) throws IllegalFilenameException {
        if (!VALID_FILENAME.matcher(filename).matches()) {
            throw new IllegalFilenameException("Invalid filename: " + filename);
        }
    }

    /**
     * Check if the username is valid and not contain any invalid characters
     *
     * @param username username
     * @throws IllegalUsernameException if the username is invalid
     */
    public static void checkValidUsername(final String username) throws IllegalUsernameException {
        if (!VALID_USERNAME.matcher(username).matches()) {
            throw new IllegalUsernameException("Invalid username: " + username);
        }
    }

    /**
     * Check if the email is valid and not contain any invalid characters
     *
     * @param email email
     * @throws IllegalEmailException if the email is invalid
     */
    public static void checkValidEmail(final String email) throws IllegalEmailException {
        if (!VALID_EMAIL.matcher(email).matches()) {
            throw new IllegalEmailException("Invalid email: " + email);
        }
    }

    /**
     * Check if the password is valid and is at least 8 characters long. Passwords may not start or end with space.
     *
     * @param password password
     * @throws IllegalPasswordException if the password is invalid
     */
    public static void checkValidPassword(final String password) throws IllegalPasswordException {
        if (password == null || password.trim().length() < 8) {
            throw new IllegalPasswordException("Password must be at least 8 characters long");
        }
    }


    /**
     * Check if the role is valid. Only NONE, ADMIN, USER and SUPERADMIN are valid roles.
     *
     * @param role role
     * @throws IllegalRoleException if the role is invalid
     */
    public static void checkValidRole(final String role) throws IllegalRoleException {
        if (role == null) {
            throw new IllegalRoleException("Role must not be null");
        }

        try {
            Role roleType = Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalRoleException("Invalid role: " + role);
        }
    }

    /**
     * Check if the role is valid for an album. Only NONE, ADMIN and USER are valid roles.
     *
     * @param role role
     * @throws IllegalRoleException if the role is invalid
     */
    public static void checkValidAlbumRole(final String role) throws IllegalRoleException {
        if (role == null) {
            throw new IllegalRoleException("Role must not be null");
        }

        try {
            Role roleType = Role.valueOf(role);

            if (roleType == Role.SUPERADMIN) {
                throw new IllegalRoleException("Role must not be SUPERADMIN");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalRoleException("Invalid role: " + role);
        }
    }

}
