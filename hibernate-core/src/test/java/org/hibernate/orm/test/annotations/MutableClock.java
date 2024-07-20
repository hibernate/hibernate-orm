/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class MutableClock extends Clock {

	private static final Instant START_INSTANT = LocalDateTime.of( 2000, 1, 1, 12, 30, 50, 123456789 )
			.toInstant( ZoneOffset.UTC );

	private final ZoneId zoneId;
	private Instant instant;

	public MutableClock() {
		this( ZoneId.systemDefault(), START_INSTANT );
	}

	public MutableClock(ZoneId zoneId) {
		this( zoneId, START_INSTANT );
	}

	public MutableClock(Instant instant) {
		this( ZoneId.systemDefault(), instant );
	}

	public MutableClock(ZoneId zoneId, Instant instant) {
		this.zoneId = zoneId;
		this.instant = instant;
	}

	@Override
	public ZoneId getZone() {
		return zoneId;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return new MutableClock( zone, instant );
	}

	@Override
	public Instant instant() {
		return instant;
	}

	public void setInstant(Instant instant) {
		this.instant = instant;
	}

	public void reset() {
		instant = START_INSTANT;
	}

	public void tick() {
		instant = instant.plusSeconds( 1 );
	}
}
