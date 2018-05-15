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
import org.hibernate.type.descriptor.sql.internal.JdbcLiteralFormatterNumericData;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#TINYINT TINYINT} handling.
 * <p/>
 * Note that <tt>JDBC</tt> states that TINYINT should be mapped to either byte or short, but points out
 * that using byte can in fact lead to loss of data.
 *
 * @author Steve Ebersole
 */
public class TinyIntSqlDescriptor extends AbstractTemplateSqlTypeDescriptor {
	public static final TinyIntSqlDescriptor INSTANCE = new TinyIntSqlDescriptor();

	public TinyIntSqlDescriptor() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TINYINT;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Short.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterNumericData( javaTypeDescriptor, Short.class );
	}

	@Override
	protected <X> JdbcValueBinder<X> createBinder(
			final BasicJavaDescriptor<X> javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(
					PreparedStatement st,
					int index,
					X value,
					ExecutionContext executionContext) throws SQLException {
				st.setByte( index, javaTypeDescriptor.unwrap( value, Byte.class, executionContext.getSession() ) );
			}

			@Override
			protected void doBind(
					CallableStatement st,
					String name,
					X value,
					ExecutionContext executionContext)
					throws SQLException {
				st.setByte( name, javaTypeDescriptor.unwrap( value, Byte.class, executionContext.getSession() ) );
			}
		};
	}

	@Override
	protected <X> JdbcValueExtractor<X> createExtractor(
			final BasicJavaDescriptor<X> javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int position, ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getByte( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(CallableStatement statement, int position, ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getByte( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getByte( name ), executionContext.getSession() );
			}
		};
	}
}
