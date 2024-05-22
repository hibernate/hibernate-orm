/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

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
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import static java.util.Collections.emptySet;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

/**
 * Represents a named {@code enum} type on Oracle 23ai+.
 * <p>
 * Hibernate does <em>not</em> automatically use this for enums
 * mapped as {@link jakarta.persistence.EnumType#STRING}, and
 * instead this type must be explicitly requested using:
 * <pre>
 * &#64;JdbcTypeCode(SqlTypes.NAMED_ENUM)
 * </pre>
 *
 * @see org.hibernate.type.SqlTypes#NAMED_ENUM
 * @see OracleDialect#getEnumTypeDeclaration(String, String[])
 * @see OracleDialect#getCreateEnumTypeCommand(String, String[])
 *
 * @author Loïc Lefèvre
 */
public class OracleEnumJdbcType implements JdbcType {

	public static final OracleEnumJdbcType INSTANCE = new OracleEnumJdbcType();

	@Override
	public int getJdbcTypeCode() {
		return Types.VARCHAR;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return NAMED_ENUM;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return (appender, value, dialect, wrapperOptions) -> appender.appendSql( dialect.getEnumTypeDeclaration( (Class<? extends Enum<?>>) javaType.getJavaType() )+"." + ((Enum<?>) value).name() );
	}

	@Override
	public String getFriendlyName() {
		return "ENUM";
	}

	@Override
	public String toString() {
		return "EnumTypeDescriptor";
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
				st.setString( index, ((Enum<?>) value).name() );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setString( name, ((Enum<?>) value).name() );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( rs.getString( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getString( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getString( name ), options );
			}
		};
	}

	@Override
	public void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			Size columnSize,
			Database database,
			JdbcTypeIndicators context) {
		addAuxiliaryDatabaseObjects( javaType, database, true );
	}

	@Override
	public void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			Size columnSize,
			Database database,
			TypeConfiguration typeConfiguration) {
		addAuxiliaryDatabaseObjects( javaType, database, true );
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
		final String[] create = getCreateEnumTypeCommand(
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

	/**
	 * Used to generate the CREATE DDL command for Data Use Case Domain based on VARCHAR2 values.
	 *
	 * @param name
	 * @param values
	 * @return the DDL command to create that enum
	 */
	public String[] getCreateEnumTypeCommand(String name, String[] values) {
		final StringBuilder domain = new StringBuilder();
		domain.append( "create domain " )
				.append( name )
				.append( " as enum (" );
		String separator = "";
		for ( String value : values ) {
			domain.append( separator ).append( value ).append("='").append(value).append("'");
			separator = ", ";
		}
		domain.append( ')' );
		return new String[] { domain.toString() };
	}
}
