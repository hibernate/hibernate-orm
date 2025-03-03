/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
