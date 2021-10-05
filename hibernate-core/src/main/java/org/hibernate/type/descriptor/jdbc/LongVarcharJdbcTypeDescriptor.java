/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;

/**
 * Descriptor for {@link Types#LONGVARCHAR LONGVARCHAR} handling.
 *
 * @author Steve Ebersole
 */
public class LongVarcharJdbcTypeDescriptor extends VarcharJdbcTypeDescriptor {
	public static final LongVarcharJdbcTypeDescriptor INSTANCE = new LongVarcharJdbcTypeDescriptor();

	public LongVarcharJdbcTypeDescriptor() {
	}

	@Override
	public String toString() {
		return "LongVarcharTypeDescriptor";
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.LONGVARCHAR;
	}

	@Override
	public JdbcTypeDescriptor resolveIndicatedType(
			JdbcTypeDescriptorIndicators indicators,
			JavaTypeDescriptor<?> domainJtd) {
		final JdbcTypeDescriptorRegistry jdbcTypeDescriptorRegistry = indicators.getTypeConfiguration()
				.getJdbcTypeDescriptorRegistry();
		return indicators.isNationalized()
				? jdbcTypeDescriptorRegistry.getDescriptor( Types.LONGNVARCHAR )
				: jdbcTypeDescriptorRegistry.getDescriptor( Types.LONGVARCHAR );
	}
}
