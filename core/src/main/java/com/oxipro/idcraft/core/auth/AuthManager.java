package com.oxipro.idcraft.core.auth;

import com.oxipro.idcraft.api.account.Account;
import com.oxipro.idcraft.api.account.IAccountRepository;
import com.oxipro.idcraft.api.auth.*;
import com.oxipro.idcraft.api.auth.decisions.AuthDecisionAllow;
import com.oxipro.idcraft.api.auth.decisions.AuthDecisionDeny;
import com.oxipro.idcraft.api.auth.decisions.AuthDecisionRequiresAuth;
import com.oxipro.idcraft.api.mojang.IMojangVerifier;
import com.oxipro.idcraft.api.mojang.MojangProfile;
import com.oxipro.idcraft.api.mojang.PremiumLookupResult;
import com.oxipro.idcraft.api.password.IPasswordHasher;
import com.oxipro.idcraft.api.session.ICacheProvider;
import com.oxipro.idcraft.api.session.Session;
import com.oxipro.idcraft.core.utils.OfflineUUIDUtil;

import java.net.InetAddress;
import java.util.Locale;
import java.util.UUID;

/**
 * Critical auth path. Fail closed: unknown identity provider state : deny.
 * AuthMode only picks assigned UUID shape; Mojang contact is independent.
 */
public class AuthManager implements IAuthManager {

    private final IAccountRepository accountRepository;
    private final ICacheProvider sessionCache;
    private final IMojangVerifier mojangVerifier;
    private final IPasswordHasher passwordHasher;
    private final AuthManagerConfig config;

    public AuthManager(
            IAccountRepository accountRepository,
            ICacheProvider sessionCache,
            IMojangVerifier mojangVerifier,
            IPasswordHasher passwordHasher,
            AuthManagerConfig config
    ) {
        this.accountRepository = accountRepository;
        this.sessionCache = sessionCache;
        this.mojangVerifier = mojangVerifier;
        this.passwordHasher = passwordHasher;
        this.config = config;
    }

    // --- proxy ---

    @Override
    public IAuthDecision resolveConnection(
            String username,
            InetAddress address,
            boolean isFloodgatePlayer,
            UUID connectionUuid
    ) {
        String ip = ipOf(address);

        // Floodgate: never change UUID, never session, never online-mode
        if (isFloodgatePlayer) {
            if (connectionUuid == null) {
                return new AuthDecisionDeny(AuthDecisionDeny.AuthDenyReason.WIRED);
            }
            if (config.getSkipAuthFloodgate()) {
                return new AuthDecisionAllow(
                        null,
                        PlayerAuthType.FLOODGATE,
                        ClientAuthSource.FLOODGATE,
                        false,
                        false
                );
            }
            return new AuthDecisionRequiresAuth(
                    null,
                    PlayerAuthType.FLOODGATE,
                    ClientAuthSource.FLOODGATE,
                    false
            );
        }

        if (connectionUuid == null) {
            return new AuthDecisionDeny(AuthDecisionDeny.AuthDenyReason.WIRED);
        }

        Account account = accountRepository.findByUsername(username);
        PremiumLookupResult lookup = mojangVerifier.lookup(username);

        // --- Existing account owns the name ---
        if (account != null) {
            if (account.isLocked()) {
                return new AuthDecisionDeny(AuthDecisionDeny.AuthDenyReason.ACCOUNT_LOCKED);
            }

            // Resync premium flag when provider is definitive
            account = resyncPremiumFlag(account, lookup);

            ClientAuthSource clientSource = clientSourceFromLookup(lookup, account);

            AuthDecisionAllow sessionHit = tryValidatedSession(
                    account.getUuid(),
                    username,
                    ip,
                    clientSource
            );
            if (sessionHit != null) {
                return sessionHit;
            }

            PlayerAuthType assignedType = deriveAssignedType(account, lookup);
            // Account password path does not need Mojang online-mode
            return new AuthDecisionRequiresAuth(
                    account.getUuid(),
                    assignedType,
                    clientSource,
                    false
            );
        }

        // --- No account: try username-indexed session (Mojang-down reconnect) ---
        AuthDecisionAllow byNameSession = tryValidatedSessionByUsername(username, ip);
        if (byNameSession != null) {
            return byNameSession;
        }

        // Provider unknown and no session: kick
        if (lookup.isUnknown()) {
            return new AuthDecisionDeny(AuthDecisionDeny.AuthDenyReason.IDENTITY_PROVIDER_UNAVAILABLE);
        }

        // --- Premium name, no account ---
        if (lookup.isPremium()) {
            MojangProfile profile = lookup.getProfile();
            UUID assignedUuid = resolveAssignedUuid(username, profile);
            boolean onlineMode = needsOnlineMode(assignedUuid, profile);

            AuthDecisionAllow sessionHit = tryValidatedSession(
                    assignedUuid,
                    username,
                    ip,
                    ClientAuthSource.MOJANG
            );
            if (sessionHit != null) {
                return sessionHit;
            }

            if (config.getSkipAuthPremium()) {
                // Do NOT put session here - only after full proof (Velocity LoginEvent)
                return new AuthDecisionAllow(
                        assignedUuid,
                        PlayerAuthType.PREMIUM,
                        ClientAuthSource.MOJANG,
                        false,
                        onlineMode
                );
            }

            return new AuthDecisionRequiresAuth(
                    assignedUuid,
                    PlayerAuthType.PREMIUM,
                    ClientAuthSource.MOJANG,
                    onlineMode
            );
        }

        // --- Not premium, no account ---
        UUID assignedUuid = resolveAssignedUuid(username, null);

        AuthDecisionAllow sessionHit = tryValidatedSession(
                assignedUuid,
                username,
                ip,
                ClientAuthSource.OFFLINE
        );
        if (sessionHit != null) {
            return sessionHit;
        }

        return new AuthDecisionRequiresAuth(
                assignedUuid,
                PlayerAuthType.CRACKED,
                ClientAuthSource.OFFLINE,
                false
        );
    }

    // --- auth server ---

    @Override
    public AuthResult register(UUID uuid, String username, String password, String confirmPassword, String ip) {
        if (password == null || password.length() < config.getMinPasswordLength()) {
            return AuthResult.failure(AuthErrorCode.PASSWORD_TOO_SHORT);
        }
        if (containsWhitespace(password)) {
            return AuthResult.failure(AuthErrorCode.PASSWORD_CONTAINS_SPACES);
        }
        if (config.isPasswordRegexEnabled()) {
            if (config.getPasswordPattern() == null
                    || !config.getPasswordPattern().matcher(password).matches()) {
                return AuthResult.failure(AuthErrorCode.PASSWORD_REGEX_MISMATCH);
            }
        }
        if (!password.equals(confirmPassword)) {
            return AuthResult.failure(AuthErrorCode.PASSWORDS_DO_NOT_MATCH);
        }
        if (accountRepository.existsByUsername(username)) {
            return AuthResult.failure(AuthErrorCode.ACCOUNT_ALREADY_EXISTS);
        }

        PremiumLookupResult lookup = mojangVerifier.lookup(username);
        if (lookup.isUnknown()) {
            return AuthResult.failure(AuthErrorCode.PROVIDER_UNAVAILABLE);
        }

        boolean premiumUuid = false;
        if (lookup.isPremium()) {
            if (lookup.getProfile().getUuid().equals(uuid)) {
                premiumUuid = true;
            }
        }

        if (lookup.isPremium()) {
            if (!premiumUuid) {
                if (!config.isAllowOnPremiumUsername()) {
                    return AuthResult.failure(AuthErrorCode.PREMIUM_USERNAME_RESERVED);
                }
            }
        }

        boolean premiumAccount = premiumUuid;
        Account account = Account.newAccount(uuid, username, passwordHasher.hash(password), ip, premiumAccount);
        accountRepository.save(account);

        PlayerAuthType type;
        if (premiumAccount) {
            type = PlayerAuthType.PREMIUM;
        } else {
            type = PlayerAuthType.CRACKED;
        }
        recordSession(uuid, username, type, ip);
        return AuthResult.ok();
    }

    @Override
    public AuthResult login(UUID uuid, String username, String password, String ip) {
        Account account = accountRepository.findByUsername(username);
        if (account == null) {
            return AuthResult.failure(AuthErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (!account.getUuid().equals(uuid)) {
            return AuthResult.failure(AuthErrorCode.UUID_MISMATCH);
        }
        if (account.isLocked()) {
            return AuthResult.failure(AuthErrorCode.ACCOUNT_LOCKED);
        }
        if (account.getLockedUntil() != null) {
            account = account.clearFailedAttempts();
            accountRepository.save(account);
        }
        if (!passwordHasher.matches(password, account.getPasswordHash())) {
            if (config.isLoginCooldownEnabled()) {
                accountRepository.save(account.withFailedAttempt(
                        config.getMaxFailedAttempts(),
                        config.getLoginCooldown()
                ));
            }
            return AuthResult.failure(AuthErrorCode.WRONG_PASSWORD);
        }

        PremiumLookupResult lookup = mojangVerifier.lookup(username);
        // UNKNOWN: still allow password login (account already proven by password)
        if (!lookup.isUnknown()) {
            account = resyncPremiumFlag(account, lookup);
        }

        accountRepository.save(account.afterSuccessfulLogin(ip));

        PlayerAuthType type = deriveAssignedType(account, lookup);
        // Prefer live Mojang UUID match when known
        if (lookup.isPremium()) {
            if (lookup.getProfile().getUuid().equals(uuid)) {
                type = PlayerAuthType.PREMIUM;
            }
        }

        recordSession(uuid, username, type, ip);
        return AuthResult.ok();
    }

    @Override
    public boolean hasActiveSession(UUID uuid, String username, String ip) {
        if (uuid == null || username == null) {
            return false;
        }
        Session session = sessionCache.get(uuid);
        return validateSession(session, username, ip) != null;
    }

    @Override
    public void recordSession(UUID uuid, String username, PlayerAuthType type, String ip) {
        if (uuid == null || username == null || type == null) {
            return;
        }
        if (ip == null || ip.isEmpty()) {
            // Sessions are always IP-bound — refuse to store without IP
            return;
        }
        if (!config.getUseSessions()) {
            return;
        }
        if (!isSessionTypeSupported(type)) {
            return;
        }
        if (type == PlayerAuthType.FLOODGATE) {
            return;
        }
        // Future: player opt-out checked here
        sessionCache.put(Session.authenticated(username, uuid, type, ip), config.getSessionTtl());
    }

    @Override
    public void invalidateSession(UUID uuid) {
        if (uuid != null) {
            sessionCache.invalidate(uuid);
        }
    }

    // --- session validation (shared) ---

    private AuthDecisionAllow tryValidatedSession(
            UUID uuid,
            String username,
            String ip,
            ClientAuthSource clientSource
    ) {
        if (uuid == null) {
            return null;
        }
        Session session = sessionCache.get(uuid);
        Session validated = validateSession(session, username, ip);
        if (validated == null) {
            return null;
        }
        return allowFromSession(validated, clientSource);
    }

    private AuthDecisionAllow tryValidatedSessionByUsername(String username, String ip) {
        if (!config.getUseSessions()) {
            return null;
        }
        Session session = sessionCache.getByUsername(username);
        Session validated = validateSession(session, username, ip);
        if (validated == null) {
            return null;
        }
        ClientAuthSource source;
        if (validated.getType() == PlayerAuthType.PREMIUM) {
            source = ClientAuthSource.MOJANG;
        } else {
            source = ClientAuthSource.OFFLINE;
        }
        return allowFromSession(validated, source);
    }

    /**
     * Full session gate: support flags, IP, username, account lock/existence rules.
     * Invalid sessions are dropped from cache when clearly corrupt.
     */
    private Session validateSession(Session session, String connectingUsername, String ip) {
        if (session == null) {
            return null;
        }
        if (!config.getUseSessions()) {
            return null;
        }
        if (!isSessionTypeSupported(session.getType())) {
            return null;
        }
        if (session.getType() == PlayerAuthType.FLOODGATE) {
            return null;
        }
        if (!ipMatches(session, ip)) {
            return null;
        }
        if (session.getUsername() == null || connectingUsername == null) {
            invalidateQuiet(session);
            return null;
        }
        if (!session.getUsername().equalsIgnoreCase(connectingUsername)) {
            invalidateQuiet(session);
            return null;
        }

        Account byUuid = accountRepository.findByUuid(session.getUuid());
        Account byName = accountRepository.findByUsername(connectingUsername);

        // Username account must own this UUID
        if (byName != null) {
            if (!byName.getUuid().equals(session.getUuid())) {
                invalidateQuiet(session);
                return null;
            }
            if (byName.isLocked()) {
                return null;
            }
        }

        if (byUuid != null) {
            if (byUuid.isLocked()) {
                return null;
            }
            if (!byUuid.getUsernameLower().equals(connectingUsername.toLowerCase(Locale.ROOT))) {
                invalidateQuiet(session);
                return null;
            }
        }

        // Cracked session only after password proof → account must exist
        if (session.getType() == PlayerAuthType.CRACKED) {
            if (byUuid == null && byName == null) {
                invalidateQuiet(session);
                return null;
            }
        }

        // PREMIUM session may exist without account (skip-auth premium)
        return session;
    }

    private AuthDecisionAllow allowFromSession(Session session, ClientAuthSource clientSource) {
        // Resume never forces online-mode (absorbs Mojang session-server downtime)
        return new AuthDecisionAllow(
                session.getUuid(),
                session.getType(),
                clientSource,
                true,
                false
        );
    }

    private void invalidateQuiet(Session session) {
        if (session != null && session.getUuid() != null) {
            sessionCache.invalidate(session.getUuid());
        }
    }

    // --- account / premium helpers ---

    private Account resyncPremiumFlag(Account account, PremiumLookupResult lookup) {
        if (account == null || lookup == null || lookup.isUnknown()) {
            return account;
        }

        boolean shouldBePremium = false;
        if (lookup.isPremium()) {
            if (lookup.getProfile().getUuid().equals(account.getUuid())) {
                shouldBePremium = true;
            }
        }

        if (account.isPremium() == shouldBePremium) {
            return account;
        }

        Account updated = account.withPremium(shouldBePremium);
        accountRepository.save(updated);
        return updated;
    }

    private PlayerAuthType deriveAssignedType(Account account, PremiumLookupResult lookup) {
        if (lookup != null && lookup.isPremium()) {
            if (lookup.getProfile().getUuid().equals(account.getUuid())) {
                return PlayerAuthType.PREMIUM;
            }
            return PlayerAuthType.CRACKED;
        }
        if (lookup != null && lookup.isNotPremium()) {
            return PlayerAuthType.CRACKED;
        }
        // UNKNOWN: trust last known DB flag only for typing RequiresAuth
        if (account.isPremium()) {
            return PlayerAuthType.PREMIUM;
        }
        return PlayerAuthType.CRACKED;
    }

    private static ClientAuthSource clientSourceFromLookup(PremiumLookupResult lookup, Account account) {
        if (lookup != null && lookup.isPremium()) {
            return ClientAuthSource.MOJANG;
        }
        if (lookup != null && lookup.isNotPremium()) {
            return ClientAuthSource.OFFLINE;
        }
        // UNKNOWN: prefer account flag for clientSource labeling only
        if (account != null && account.isPremium()) {
            return ClientAuthSource.MOJANG;
        }
        return ClientAuthSource.OFFLINE;
    }

    private boolean isSessionTypeSupported(PlayerAuthType type) {
        if (type == null) {
            return false;
        }
        if (type == PlayerAuthType.CRACKED) {
            return config.isSessionSupportCrack();
        }
        if (type == PlayerAuthType.PREMIUM) {
            return config.isSessionSupportPremium();
        }
        return false;
    }

    private boolean ipMatches(Session session, String ip) {
        if (session.getIp() == null || session.getIp().isEmpty()) {
            return false;
        }
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return session.getIp().equals(ip);
    }

    // AuthMode → UUID shape only
    private UUID resolveAssignedUuid(String username, MojangProfile premiumProfile) {
        AuthMode mode = config.getAuthMode();

        if (mode == AuthMode.CRACKED) {
            return OfflineUUIDUtil.generate(username);
        }

        if (mode == AuthMode.MIXED) {
            if (premiumProfile != null) {
                return premiumProfile.getUuid();
            }
            return OfflineUUIDUtil.generate(username);
        }

        // RANDOM
        return UUID.randomUUID();
    }

    // Online-mode only when assigned UUID is the real Mojang UUID (typically MIXED)
    private boolean needsOnlineMode(UUID assignedUuid, MojangProfile premiumProfile) {
        if (premiumProfile == null || assignedUuid == null) {
            return false;
        }
        return premiumProfile.getUuid().equals(assignedUuid);
    }

    private static String ipOf(InetAddress address) {
        if (address == null) {
            return null;
        }
        return address.getHostAddress();
    }

    private static boolean containsWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
