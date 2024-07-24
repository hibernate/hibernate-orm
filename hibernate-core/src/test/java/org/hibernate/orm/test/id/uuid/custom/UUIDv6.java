package org.hibernate.orm.test.id.uuid.custom;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Random;
import java.util.UUID;

public class UUIDv6 {

    private static final Random RND = new SecureRandom();

    private static final Instant EPOCH_1582 = LocalDate.of(1582, 10, 15).atStartOfDay(ZoneId.of("UTC")).toInstant();

    private static class State {

        private long currentTimestamp;
        private long clockSequence;

        private State() {
            currentTimestamp = getCurrentTimestamp();
            clockSequence = RND.nextLong() & 0x3FFF;
        }

        public State(final long currentTimestamp, final long clockSequence) {
            this.currentTimestamp = currentTimestamp;
            this.clockSequence = clockSequence;
        }

        public long getSequence(final long currentTimestamp) {
            if (this.currentTimestamp > currentTimestamp) {
                clockSequence = RND.nextLong() & 0x3FFF;
            } else {
                clockSequence++;
                clockSequence &= 0x3FFF;
            }
            this.currentTimestamp = currentTimestamp;
            return clockSequence;
        }
    }

    private static State state = new State();

    public static void initState(final long timestamp, final long sequence) {
        state = new State(timestamp, sequence);
    }

    private static long getCurrentTimestamp() {
        final var duration = Duration.between(EPOCH_1582, Instant.now());
        return duration.toSeconds() * 10_000_000 + duration.toNanosPart() / 100;
    }

    public static UUID nextIdentifier() {
        final var currentTimestamp = getCurrentTimestamp();

        final var state = UUIDv6.state.getSequence(currentTimestamp);
        final var node = RND.nextLong() & 0xFFFF_FFFF_FFFFL;

        return new UUID(
            currentTimestamp << 4 & 0xFFFF_FFFF_FFFF_0000L | 0x6000L | currentTimestamp & 0x0FFFL,
            0x8000_0000_0000_0000L | (state & 0x3FFFL) << 48 | node);
    }

    public static long timestamp(final UUID uuid) {
        if (uuid.version() != 6) {
            return uuid.timestamp();
        }
        final var msb = uuid.getMostSignificantBits();
        return msb >> 4 & 0x0FFF_FFFF_FFFF_F000L | msb & 0x0FFFL;
    }

    public static Instant instant(final UUID uuid) {
        final var ts = timestamp(uuid);
        return EPOCH_1582.plusSeconds(ts / 10_000_000).plusNanos(ts % 10_000_000 * 100);
    }

    public static int clockSequence(final UUID uuid) {
        if (uuid.version() != 6) {
            return uuid.clockSequence();
        }
        return (int) ((uuid.getLeastSignificantBits() & 0x3FFF_0000_0000_0000L) >>> 48);
    }

    public static long node(final UUID uuid) {
        if (uuid.version() != 6) {
            return uuid.node();
        }
        return uuid.getLeastSignificantBits() & 0x0000_FFFF_FFFF_FFFFL;
    }
}
