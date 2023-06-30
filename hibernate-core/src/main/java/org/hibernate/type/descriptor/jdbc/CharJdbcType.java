/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#CHAR CHAR} handling.
 *
 * @author Steve Ebersole
 */
public class CharJdbcType extends VarcharJdbcType {
	public static final CharJdbcType INSTANCE = new CharJdbcType();

	public CharJdbcType() {
	}

	@Override
	public String toString() {
		return "CharTypeDescriptor";
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.CHAR;
	}

	@Override
	protected int resolveIndicatedJdbcTypeCode(JdbcTypeIndicators indicators) {
		if ( indicators.isLob() ) {
			return indicators.isNationalized() ? Types.NCLOB : Types.CLOB;
		}
		else {
			return indicators.isNationalized() ? Types.NCHAR : Types.CHAR;
		}
	}
}
