/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;
import java.time.Duration;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link java.time.Duration}.
 *
 * @author Marco Belladelli
 */
public class DurationJdbcType extends NumericJdbcType {
	public static final DurationJdbcType INSTANCE = new DurationJdbcType();

	@Override
	public int getDdlTypeCode() {
		return Types.NUMERIC;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.DURATION;
	}

	@Override
	public String getFriendlyName() {
		return "DURATION";
	}

	@Override
	public String toString() {
		return "DurationJdbcType";
	}
}
