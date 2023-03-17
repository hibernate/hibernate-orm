/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#TINYINT TINYINT} handling, when the {@link Types#SMALLINT SMALLINT} DDL type code is used.
 * This type extends {@link SmallIntJdbcType}, because on the JDBC side we must bind {@link Short} instead of {@link Byte},
 * because it is not specified whether the conversion from {@link Byte} to {@link Short} is signed or unsigned,
 * and we need the conversion to be signed, which is properly handled by the {@link org.hibernate.type.descriptor.java.JavaType#unwrap(Object, Class, WrapperOptions)} implementations.
 */
public class TinyIntAsSmallIntJdbcType extends SmallIntJdbcType {
	public static final TinyIntAsSmallIntJdbcType INSTANCE = new TinyIntAsSmallIntJdbcType();

	public TinyIntAsSmallIntJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TINYINT;
	}

	@Override
	public int getDdlTypeCode() {
		return Types.SMALLINT;
	}

	@Override
	public String getFriendlyName() {
		return "TINYINT";
	}

	@Override
	public String toString() {
		return "TinyIntAsSmallIntTypeDescriptor";
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().getDescriptor( Byte.class );
	}
}
