/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Types;

/**
 * Factory for {@link OracleArrayJdbcType}.
 *
 * @see OracleJdbcHelper#getArrayJdbcTypeConstructor
 *
 * @author Gavin King
 */
public class OracleArrayJdbcTypeConstructor implements JdbcTypeConstructor {
	@Override
	public JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect, BasicType<?> elementType,
			ColumnTypeInformation columnTypeInformation) {
		String typeName = columnTypeInformation == null ? null : columnTypeInformation.getTypeName();
		if ( typeName == null || typeName.isBlank() ) {
			typeName = OracleArrayJdbcType.getTypeName( elementType.getJavaTypeDescriptor(), dialect );
		}
//		if ( typeName == null ) {
//			// Fallback to XML type for the representation of arrays as the native JSON type was only introduced in 21
//			// Also, use the XML type if the Oracle JDBC driver classes are not visible
//			return typeConfiguration.getJdbcTypeRegistry().getDescriptor( SqlTypes.SQLXML );
//		}
		return new OracleArrayJdbcType( elementType.getJdbcType(), typeName );
	}

	@Override
	public JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			JdbcType elementType,
			ColumnTypeInformation columnTypeInformation) {
		// a bit wrong, since columnTypeInformation.getTypeName() is typically null!
		String typeName = columnTypeInformation == null ? null : columnTypeInformation.getTypeName();
		if ( typeName == null || typeName.isBlank() ) {
			Integer precision = null;
			Integer scale = null;
			if ( columnTypeInformation != null ) {
				precision = columnTypeInformation.getColumnSize();
				scale = columnTypeInformation.getDecimalDigits();
			}
			typeName = OracleArrayJdbcType.getTypeName( elementType.getJdbcRecommendedJavaTypeMapping(
					precision,
					scale,
					typeConfiguration
			), dialect );
		}
		return new OracleArrayJdbcType( elementType, typeName );
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return Types.ARRAY;
	}
}
