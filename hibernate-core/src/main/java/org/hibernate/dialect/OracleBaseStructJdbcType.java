/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UserDefinedObjectType;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;

import oracle.sql.TIMESTAMPTZ;

/**
 * @author Christian Beikov
 */
public class OracleBaseStructJdbcType extends StructJdbcType {

	public OracleBaseStructJdbcType() {
		// The default instance is for reading only and will return an Object[]
		this( null, null, null );
	}

	protected OracleBaseStructJdbcType(EmbeddableMappingType embeddableMappingType, String typeName, int[] orderMapping) {
		super(
				embeddableMappingType,
				typeName == null ? null : typeName.toUpperCase( Locale.ROOT ),
				orderMapping
		);
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setObject( index, createJdbcValue( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, createJdbcValue( value, options ) );
			}

			@Override
			public Object getBindValue(X value, WrapperOptions options) throws SQLException {
				return createJdbcValue( value, options );
			}
		};
	}

	@Override
	public String getExtraCreateTableInfo(
			JavaType<?> javaType,
			String columnName,
			String tableName,
			Database database) {
		final UserDefinedObjectType udt = database.getDefaultNamespace()
				.locateUserDefinedType( Identifier.toIdentifier( getSqlTypeName() ) );
		StringBuilder sb = null;
		for ( Column column : udt.getColumns() ) {
			final Type columnType = column.getValue().getType();
			if ( columnType instanceof JdbcMapping ) {
				final JdbcMapping jdbcMapping = (JdbcMapping) columnType;
				final String extraCreateTableInfo = jdbcMapping.getJdbcType().getExtraCreateTableInfo(
						jdbcMapping.getJavaTypeDescriptor(),
						columnName + "." + column.getName(),
						tableName,
						database
				);
				if ( !extraCreateTableInfo.isEmpty() ) {
					if ( sb == null ) {
						sb = new StringBuilder();
					}
					else {
						sb.append( ',' );
					}
					sb.append( extraCreateTableInfo );
				}
			}
		}
		return sb != null ? sb.toString() : "";
	}
}
