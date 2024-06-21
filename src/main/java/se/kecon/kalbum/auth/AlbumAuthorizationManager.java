package se.kecon.kalbum.auth;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Component("albumAuthorizationManager")
public class AlbumAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    @Autowired
    private UserDao userDao;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, MethodInvocation object) {
        log.info("check {} {} {}", authentication.get().getName(), object.getMethod().getName(), object.getArguments()); // TODO: verify if needed
        return new AuthorizationDecision(true);
    }

    @Override
    public void verify(Supplier<Authentication> authentication, MethodInvocation object) {
        log.info("verify {} {} {}", authentication.get().getName(), object.getMethod().getName(), object.getArguments()); // TODO: verify if needed
        AuthorizationManager.super.verify(authentication, object);
    }

    public boolean isAlbumUser(Authentication authentication, String albumId) {

        Optional<User> user = this.userDao.findByUsername(authentication.getName());

        if (user.isEmpty()) {
            log.info("isUser {} {} rejected due to no valid user", authentication.getName(), albumId);
            return false;
        }

        if (user.get().getRole() == Role.SUPERADMIN) {
            log.info("isUser {} {} accepted as super user", authentication.getName(), albumId);
            return true;
        }

        final Role albumRole = user.get().getAlbumRoles().getOrDefault(albumId, Role.NONE);

        if (albumRole != Role.NONE) {
            log.info("isUser {} {} accepted as {}", authentication.getName(), albumId, albumRole);
            return true;
        }

        log.info("isUser {} {} rejected", authentication.getName(), albumId);
        return false;
    }

    public boolean isAlbumAdmin(Authentication authentication, String albumId) {
        final Optional<User> user = this.userDao.findByUsername(authentication.getName());

        if (user.isEmpty()) {
            log.info("isAlbumAdmin {} {} rejected due to no valid user", authentication.getName(), albumId);
            return false;
        }

        final Role userRole = user.get().getRole();

        if (userRole == Role.SUPERADMIN) {
            log.info("isAlbumAdmin {} {} accepted as super user", authentication.getName(), albumId);
            return true;
        }

        final Role albumRole = user.get().getAlbumRoles().getOrDefault(albumId, Role.NONE);

        if (albumRole == Role.ADMIN) {
            log.info("isAlbumAdmin {} {} accepted as {}", authentication.getName(), albumId, albumRole);
            return true;
        }

        log.info("isAlbumAdmin {} {} rejected", authentication.getName(), albumId);
        return false;
    }

    public boolean isAdmin(Authentication authentication) {
        log.warn("isAdmin {}", authentication);
        final Optional<User> user = this.userDao.findByUsername(authentication.getName());

        if (user.isEmpty()) {
            log.info("isAdmin {} rejected due to no valid user", authentication.getName());
            return false;
        }

        final Role userRole = user.get().getRole();

        if (userRole == Role.SUPERADMIN) {
            log.info("isAdmin {} accepted as super user", authentication.getName());
            return true;
        }

        if (userRole == Role.ADMIN) {
            log.info("isAdmin {} accepted as admin", authentication.getName());
            return true;
        }

        log.info("isAdmin {} rejected", authentication.getName());
        return false;
    }

    public boolean canAssignRole(Authentication authentication, String username, String role) {

        final Optional<User> user = this.userDao.findByUsername(username);
        final Optional<User> authUser = this.userDao.findByUsername(authentication.getName());

        if (authUser.isEmpty()) {
            log.info("canAssignRole {} {} {} rejected due to no valid user", authentication.getName(), username, role);
            return false;
        }

        if (user.isEmpty()) {
            log.info("canAssignRole {} {} {} rejected due to no valid user", authentication.getName(), username, role);
            return false;
        }

        final Role authRole = authUser.get().getRole();
        final Role newRole = Role.valueOf(role);
        final Role userRole = user.get().getRole();

        if (authRole == Role.SUPERADMIN) {
            log.info("canAssignRole {} {} {} accepted as super user", authentication.getName(), username, role);
            return true;
        }

        if (authRole == Role.ADMIN && userRole != Role.SUPERADMIN) {
            if (switch (newRole) {
                case USER, ADMIN -> true;
                default -> false;
            }) {
                log.info("canAssignRole {} {} {} accepted as admin", authentication.getName(), username, role);
                return true;
            }
        }

        log.info("canAssignRole {} {} {} rejected", authentication.getName(), username, role);
        return false;
    }

    public boolean isSelfOrAdmin(Authentication authentication, String username) {

        final Optional<User> user = this.userDao.findByUsername(username);
        final Optional<User> authUser = this.userDao.findByUsername(authentication.getName());

        if (authUser.isEmpty()) {
            log.info("isSelfOrAdmin {} {} rejected due to no valid user", authentication.getName(), username);
            return false;
        }

        if (user.isEmpty()) {
            log.info("isSelfOrAdmin {} {} rejected due to no valid user", authentication.getName(), username);
            return false;
        }

        final Role authRole = authUser.get().getRole();
        final Role userRole = user.get().getRole();

        if (authRole == Role.SUPERADMIN || authRole == Role.ADMIN) {
            log.info("isSelfOrAdmin {} {} accepted as admin", authentication.getName(), username);
            return true;
        }

        if (authentication.getName().equals(username)) {
            log.info("isSelfOrAdmin {} {} accepted as self", authentication.getName(), username);
            return true;
        }

        log.info("isSelfOrAdmin {} {} rejected", authentication.getName(), username);
        return false;
    }

    public boolean isSelf(Authentication authentication, String username) {

        final Optional<User> user = this.userDao.findByUsername(username);
        final Optional<User> authUser = this.userDao.findByUsername(authentication.getName());

        if (authUser.isEmpty()) {
            log.info("isSelf {} {} rejected due to no valid user", authentication.getName(), username);
            return false;
        }

        if (user.isEmpty()) {
            log.info("isSelf {} {} rejected due to no valid user", authentication.getName(), username);
            return false;
        }

        if (authentication.getName().equals(username)) {
            log.info("isSelf {} {} accepted as self", authentication.getName(), username);
            return true;
        }

        log.info("isSelf {} {} rejected", authentication.getName(), username);
        return false;
    }

}
