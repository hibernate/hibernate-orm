/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static java.util.Collections.emptySet;
import static org.hibernate.type.SqlTypes.ENUM;

/**
 * @author Gavin King
 */
public class PostgreSQLEnumJdbcType implements JdbcType {

	@Override
	public int getJdbcTypeCode() {
		return ENUM;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return (appender, value, dialect, wrapperOptions) -> appender.appendSql( "'" + ((Enum<?>) value).name() + "'::"
				+ dialect.getEnumTypeDeclaration( (Class<? extends Enum<?>>) javaType.getJavaType() ) );
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
				st.setNull( index, Types.OTHER );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, Types.OTHER );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setObject( index, value, Types.OTHER );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, value, Types.OTHER );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( rs.getObject( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getObject( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getObject( name ), options );
			}
		};
	}

	@Override
	public void addAuxiliaryDatabaseObjects(JavaType<?> javaType, InFlightMetadataCollector metadataCollector) {
		Database database = metadataCollector.getDatabase();
		Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) javaType.getJavaType();
		String name = enumClass.getSimpleName();
		String[] create = database.getDialect().getCreateEnumTypeCommand( enumClass );
		if ( create != null ) {
			String[] drop = database.getDialect().getDropEnumTypeCommand( enumClass );
			database.addAuxiliaryDatabaseObject(
					new NamedAuxiliaryDatabaseObject( name, database.getDefaultNamespace(), create, drop, emptySet(), true )
			);
		}
	}
}
