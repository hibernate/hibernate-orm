/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.Incubating;

/**
 * Describes the extent to which a given database supports the SQL
 * {@code with time zone} types.
 * <p>
 * Really we only care about {@code timestamp with time zone} here,
 * since the type {@code time with time zone} is deeply conceptually
 * questionable, and so Hibernate eschews its use.
 *
 * @author Christian Beikov
 */
@Incubating
public enum TimeZoneSupport {
	/**
	 * The {@code with time zone} types retain the time zone information.
	 * That is, a round trip writing and reading a zoned datetime results
	 * in the exact same zoned datetime with the same timezone.
	 */
	NATIVE,
	/**
	 * The {@code with time zone} types normalize to UTC. That is, a round
	 * trip writing and reading a zoned datetime results in a datetime
	 * representing the same instant, but in the timezone UTC.
	 */
	NORMALIZE,
	/**
	 * No support for {@code with time zone} types.
	 */
	NONE
}
