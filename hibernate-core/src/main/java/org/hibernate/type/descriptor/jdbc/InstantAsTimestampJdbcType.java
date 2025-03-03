/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.type.SqlTypes;

/**
 * Descriptor for {@link SqlTypes#TIMESTAMP_UTC TIMESTAMP_UTC} handling.
 *
 * @deprecated Use {@link TimestampUtcAsJdbcTimestampJdbcType}
 * @author Christian Beikov
 */
@Deprecated(since="6.4", forRemoval = true)
public class InstantAsTimestampJdbcType extends TimestampUtcAsJdbcTimestampJdbcType {
	public static final InstantAsTimestampJdbcType INSTANCE = new InstantAsTimestampJdbcType();
}
