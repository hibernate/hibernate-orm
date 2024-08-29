/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.type.SqlTypes;

/**
 * Descriptor for {@link SqlTypes#TIMESTAMP_UTC TIMESTAMP_UTC} handling.
 *
 * @deprecated Use {@link TimestampUtcAsOffsetDateTimeJdbcType}
 * @author Christian Beikov
 */
@Deprecated(since="6.4", forRemoval = true)
public class InstantAsTimestampWithTimeZoneJdbcType extends TimestampUtcAsOffsetDateTimeJdbcType {
	public static final InstantAsTimestampWithTimeZoneJdbcType INSTANCE = new InstantAsTimestampWithTimeZoneJdbcType();
}
