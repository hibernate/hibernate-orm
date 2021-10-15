/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#NCHAR NCHAR} handling.
 *
 * @author Steve Ebersole
 */
public class NCharJdbcType extends NVarcharJdbcType {
	public static final NCharJdbcType INSTANCE = new NCharJdbcType();

	public NCharJdbcType() {
	}

	@Override
	public String toString() {
		return "NCharTypeDescriptor";
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.NCHAR;
	}

	@Override
	public JdbcType resolveIndicatedType(
			JdbcTypeDescriptorIndicators indicators,
			JavaType<?> domainJtd) {
		assert domainJtd != null;

		final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
		final JdbcTypeDescriptorRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeDescriptorRegistry();

		final int jdbcTypeCode;
		if ( indicators.isLob() ) {
			jdbcTypeCode = indicators.isNationalized() ? Types.NCLOB : Types.CLOB;
		}
		else {
			jdbcTypeCode = indicators.isNationalized() ? Types.NCHAR : Types.CHAR;
		}

		return jdbcTypeRegistry.getDescriptor( jdbcTypeCode );
	}
}
