/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.sql.spi;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.sql.AbstractJdbcValueBinder;
import org.hibernate.sql.AbstractJdbcValueExtractor;
import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.descriptor.java.internal.ObjectJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for
 * @author Steve Ebersole
 */
public class ObjectSqlTypeDescriptor extends AbstractTemplateSqlTypeDescriptor {
	/**
	 * Singleton access - mapped to {@link Types#JAVA_OBJECT}
	 */
	public static final ObjectSqlTypeDescriptor INSTANCE = new ObjectSqlTypeDescriptor();

	private final int jdbcTypeCode;

	public ObjectSqlTypeDescriptor(int jdbcTypeCode) {
		this.jdbcTypeCode = jdbcTypeCode;
	}

	public ObjectSqlTypeDescriptor() {
		this( Types.JAVA_OBJECT );
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcTypeCode;
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return ObjectJavaDescriptor.INSTANCE;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		// obviously no literal support here :)
		return null;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> JdbcValueBinder<X> createBinder(
			BasicJavaDescriptor<X> javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaType() ) ) {
			return VarbinarySqlDescriptor.INSTANCE
					.getSqlExpressableType( javaTypeDescriptor, typeConfiguration )
					.getJdbcValueBinder();
		}

		return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(
					PreparedStatement st,
					int index, X value,
					ExecutionContext executionContext) throws SQLException {
				st.setObject( index, value, jdbcTypeCode );
			}

			@Override
			protected void doBind(
					CallableStatement st,
					String name,
					X value,
					ExecutionContext executionContext)
					throws SQLException {
				st.setObject( name, value, jdbcTypeCode );
			}
		};
	}


	@Override
	@SuppressWarnings("unchecked")
	public <X> JdbcValueExtractor<X> createExtractor(
			BasicJavaDescriptor<X> javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaType() ) ) {
			return VarbinarySqlDescriptor.INSTANCE
					.getSqlExpressableType( javaTypeDescriptor, typeConfiguration )
					.getJdbcValueExtractor();
		}

		return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int position, ExecutionContext executionContext)
					throws SQLException {
				return (X) rs.getObject( position );
			}

			@Override
			protected X doExtract(CallableStatement statement, int position, ExecutionContext executionContext)
					throws SQLException {
				return (X) statement.getObject( position );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, ExecutionContext executionContext)
					throws SQLException {
				return (X) statement.getObject( name );
			}
		};
	}
}
