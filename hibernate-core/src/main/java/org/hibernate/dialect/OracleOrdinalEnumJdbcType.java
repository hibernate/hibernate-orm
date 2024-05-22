/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import jakarta.persistence.EnumType;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.internal.EnumHelper;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import static java.util.Collections.emptySet;
import static org.hibernate.type.SqlTypes.NAMED_ORDINAL_ENUM;

/**
 * Represents a named {@code enum} type on Oracle 23ai+.
 * <p>
 * Hibernate does <em>not</em> automatically use this for enums
 * mapped as {@link EnumType#ORDINAL}, and
 * instead this type must be explicitly requested using:
 * <pre>
 * &#64;JdbcTypeCode(SqlTypes.NAMED_ORDINAL_ENUM)
 * </pre>
 *
 * @see org.hibernate.type.SqlTypes#NAMED_ORDINAL_ENUM
 * @see OracleDialect#getEnumTypeDeclaration(String, String[])
 * @see OracleDialect#getCreateEnumTypeCommand(String, String[])
 *
 * @author Loïc Lefèvre
 */
public class OracleOrdinalEnumJdbcType extends OracleEnumJdbcType {

	public static final OracleOrdinalEnumJdbcType INSTANCE = new OracleOrdinalEnumJdbcType();

	@Override
	public int getJdbcTypeCode() {
		return Types.INTEGER;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return NAMED_ORDINAL_ENUM;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, getJdbcTypeCode() );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, getJdbcTypeCode() );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setInt( index, ((Enum<?>) value).ordinal()+1 );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setInt( name, ((Enum<?>) value).ordinal()+1 );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				final int value = rs.getInt( paramIndex );
				if(rs.wasNull()) {
					return getJavaType().wrap(null, options);
				}
				else {
					return getJavaType().wrap(value - 1, options);
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getInt( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getInt( name ), options );
			}
		};
	}

	@Override
	public void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			Size columnSize,
			Database database,
			JdbcTypeIndicators context) {
		addAuxiliaryDatabaseObjects( javaType, database, false );
	}

	@Override
	public void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			Size columnSize,
			Database database,
			TypeConfiguration typeConfiguration) {
		addAuxiliaryDatabaseObjects( javaType, database, false );
	}

	private void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			Database database,
			boolean sortEnumValues) {
		final Dialect dialect = database.getDialect();
		final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) javaType.getJavaType();
		final String enumTypeName = enumClass.getSimpleName();
		final String[] enumeratedValues = EnumHelper.getEnumeratedValues( enumClass );
		if ( sortEnumValues ) {
			Arrays.sort( enumeratedValues );
		}
		final String[] create = dialect.getCreateEnumTypeCommand(
				javaType.getJavaTypeClass().getSimpleName(),
				enumeratedValues
		);
		final String[] drop = dialect.getDropEnumTypeCommand( enumClass );
		if ( create != null && create.length > 0 ) {
			database.addAuxiliaryDatabaseObject(
					new NamedAuxiliaryDatabaseObject(
							enumTypeName,
							database.getDefaultNamespace(),
							create,
							drop,
							emptySet(),
							true
					)
			);
		}
	}
}
