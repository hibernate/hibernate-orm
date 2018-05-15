/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

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
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#BIT BIT} handling.
 * <p/>
 * Note that JDBC is very specific about its use of the type BIT to mean a single binary digit, whereas
 * SQL defines BIT having a parameterized length.
 *
 * @author Steve Ebersole
 */
public class BitSqlDescriptor extends AbstractTemplateSqlTypeDescriptor {
	public static final BitSqlDescriptor INSTANCE = new BitSqlDescriptor();

	public BitSqlDescriptor() {
	}

	public int getJdbcTypeCode() {
		return Types.BIT;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Boolean.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		// todo : how to handle this
		return null;
	}

	@Override
	protected <X> JdbcValueBinder<X> createBinder(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
		return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(
					PreparedStatement st,
					int index, X value,
					ExecutionContext executionContext) throws SQLException {
				st.setBoolean( index, javaTypeDescriptor.unwrap( value, Boolean.class, executionContext.getSession() ) );
			}

			@Override
			protected void doBind(
					CallableStatement st,
					String name, X value,
					ExecutionContext executionContext) throws SQLException {
				st.setBoolean( name, javaTypeDescriptor.unwrap( value, Boolean.class, executionContext.getSession() ) );
			}
		};
	}

	@Override
	protected <X> JdbcValueExtractor<X> createExtractor(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
		return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int position, ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getBoolean( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(
					CallableStatement statement,
					int position,
					ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBoolean( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(
					CallableStatement statement,
					String name,
					ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBoolean( name ), executionContext.getSession() );
			}
		};
	}
}
