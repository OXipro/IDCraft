package com.oxipro.idcraft.plugin.velocity.auth;

import com.oxipro.idcraft.api.auth.ClientAuthSource;
import com.oxipro.idcraft.api.auth.IAuthDecision;
import com.oxipro.idcraft.api.auth.PlayerAuthType;
import com.oxipro.idcraft.api.auth.decisions.AuthDecisionAllow;
import com.oxipro.idcraft.api.auth.decisions.AuthDecisionDeny;
import com.oxipro.idcraft.api.auth.decisions.AuthDecisionRequiresAuth;
import com.velocitypowered.api.proxy.InboundConnection;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// Proxy-local auth state for one connection
public final class PlayerAuthContext {

    public static final class Entry {

        // What we assigned (forwarding / sessions)
        private final PlayerAuthType assignedType;
        // What the client presented
        private final ClientAuthSource clientSource;
        private final boolean needsAuth;
        private final boolean fromSession;
        // null = do not rewrite profile UUID
        private final UUID assignedUuid;
        private final boolean requiresOnlineMode;

        public Entry(
                PlayerAuthType assignedType,
                ClientAuthSource clientSource,
                boolean needsAuth,
                boolean fromSession,
                UUID assignedUuid,
                boolean requiresOnlineMode
        ) {
            this.assignedType = Objects.requireNonNull(assignedType, "assignedType");
            this.clientSource = Objects.requireNonNull(clientSource, "clientSource");
            this.needsAuth = needsAuth;
            this.fromSession = fromSession;
            this.assignedUuid = assignedUuid;
            this.requiresOnlineMode = requiresOnlineMode;
        }

        public PlayerAuthType getAssignedType() {
            return assignedType;
        }

        // Alias used by forwarding
        public PlayerAuthType getAuthType() {
            return assignedType;
        }

        public ClientAuthSource getClientSource() {
            return clientSource;
        }

        public boolean needsAuth() {
            return needsAuth;
        }

        public boolean isFromSession() {
            return fromSession;
        }

        public UUID getAssignedUuid() {
            return assignedUuid;
        }

        public UUID getResolvedUuid() {
            return assignedUuid;
        }

        public boolean requiresOnlineMode() {
            return requiresOnlineMode;
        }

        public Entry withNeedsAuth(boolean needsAuth) {
            return new Entry(assignedType, clientSource, needsAuth, fromSession, assignedUuid, requiresOnlineMode);
        }

        public Entry withAssignedUuid(UUID assignedUuid) {
            return new Entry(assignedType, clientSource, needsAuth, fromSession, assignedUuid, requiresOnlineMode);
        }
    }

    private static final class Pending {
        final IAuthDecision decision;
        final Entry entry;

        Pending(IAuthDecision decision, Entry entry) {
            this.decision = decision;
            this.entry = entry;
        }
    }

    // PreLogin -> GameProfile
    private final ConcurrentMap<InboundConnection, Pending> pending = new ConcurrentHashMap<>();

    // After profile -> disconnect
    private final ConcurrentMap<UUID, Entry> byUuid = new ConcurrentHashMap<>();

    private final ConcurrentMap<UUID, AccountDeskVisit> deskVisits = new ConcurrentHashMap<>();

    public static final class AccountDeskVisit {
        private final String previousServer;

        public AccountDeskVisit(String previousServer) {
            this.previousServer = previousServer;
        }

        public String previousServer() {
            return previousServer;
        }
    }

    public void putPending(InboundConnection connection, IAuthDecision decision) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(decision, "decision");

        if (decision instanceof AuthDecisionDeny) {
            pending.remove(connection);
            return;
        }

        pending.put(connection, new Pending(decision, toEntry(decision)));
    }

    public Optional<IAuthDecision> getPendingDecision(InboundConnection connection) {
        Pending p = pending.get(connection);
        if (p == null) {
            return Optional.empty();
        }
        return Optional.of(p.decision);
    }

    public Optional<Entry> getPendingEntry(InboundConnection connection) {
        Pending p = pending.get(connection);
        if (p == null) {
            return Optional.empty();
        }
        return Optional.of(p.entry);
    }

    public void removePending(InboundConnection connection) {
        if (connection != null) {
            pending.remove(connection);
        }
    }

    public Optional<Entry> promote(InboundConnection connection, UUID finalUuid) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(finalUuid, "finalUuid");

        Pending p = pending.remove(connection);
        if (p == null) {
            return Optional.empty();
        }

        Entry entry = p.entry;
        if (entry.getAssignedUuid() == null || !finalUuid.equals(entry.getAssignedUuid())) {
            entry = entry.withAssignedUuid(finalUuid);
        }
        byUuid.put(finalUuid, entry);
        return Optional.of(entry);
    }

    public void put(UUID uuid, Entry entry) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(entry, "entry");
        byUuid.put(uuid, entry);
    }

    public Optional<Entry> get(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byUuid.get(uuid));
    }

    public Optional<PlayerAuthType> getAuthType(UUID uuid) {
        return get(uuid).map(Entry::getAuthType);
    }

    public boolean needsAuth(UUID uuid) {
        return get(uuid).map(Entry::needsAuth).orElse(false);
    }

    public void clearNeedsAuth(UUID uuid) {
        if (uuid == null) {
            return;
        }
        byUuid.computeIfPresent(uuid, (id, entry) -> {
            if (entry.needsAuth()) {
                return entry.withNeedsAuth(false);
            }
            return entry;
        });
    }

    public void remove(UUID uuid) {
        if (uuid != null) {
            byUuid.remove(uuid);
            deskVisits.remove(uuid);
        }
    }

    public void beginAccountDesk(UUID uuid, String previousServer) {
        if (uuid == null) {
            return;
        }
        deskVisits.put(uuid, new AccountDeskVisit(previousServer));
    }

    public boolean isAccountDesk(UUID uuid) {
        return uuid != null && deskVisits.containsKey(uuid);
    }

    public Optional<AccountDeskVisit> getAccountDesk(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(deskVisits.get(uuid));
    }

    public Optional<String> getPreviousServer(UUID uuid) {
        return getAccountDesk(uuid).map(AccountDeskVisit::previousServer);
    }

    public void clearAccountDesk(UUID uuid) {
        if (uuid != null) {
            deskVisits.remove(uuid);
        }
    }

    public void clear() {
        pending.clear();
        byUuid.clear();
        deskVisits.clear();
    }

    private static Entry toEntry(IAuthDecision decision) {
        if (decision instanceof AuthDecisionAllow allow) {
            return new Entry(
                    allow.getAssignedType(),
                    allow.getClientSource(),
                    false,
                    allow.isFromSession(),
                    allow.getUuid(),
                    allow.requiresOnlineMode()
            );
        }
        if (decision instanceof AuthDecisionRequiresAuth requires) {
            return new Entry(
                    requires.getAssignedType(),
                    requires.getClientSource(),
                    true,
                    false,
                    requires.getUuid(),
                    requires.requiresOnlineMode()
            );
        }
        throw new IllegalArgumentException("Cannot build Entry from " + decision.getClass().getName());
    }
}
