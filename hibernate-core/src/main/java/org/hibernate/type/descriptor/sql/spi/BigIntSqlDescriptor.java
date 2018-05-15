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
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.internal.JdbcLiteralFormatterNumericData;

/**
 * Descriptor for {@link Types#BIGINT BIGINT} handling.
 *
 * @author Steve Ebersole
 */
public class BigIntSqlDescriptor extends AbstractTemplateSqlTypeDescriptor {
	public static final BigIntSqlDescriptor INSTANCE = new BigIntSqlDescriptor();

	public BigIntSqlDescriptor() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.BIGINT;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterNumericData( javaTypeDescriptor, Long.class );
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Long.class );
	}


	@Override
	protected <X> JdbcValueBinder<X> createBinder(
			BasicJavaDescriptor<X> javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(
					PreparedStatement st,
					int index,
					X value,
					ExecutionContext executionContext) throws SQLException {
				st.setLong( index, javaTypeDescriptor.unwrap( value, Long.class, executionContext.getSession() ) );
			}

			@Override
			protected void doBind(
					CallableStatement st,
					String name,
					X value,
					ExecutionContext executionContext)
					throws SQLException {
				st.setLong( name, javaTypeDescriptor.unwrap( value, Long.class, executionContext.getSession() ) );

			}
		};
	}

	@Override
	protected <X> JdbcValueExtractor<X> createExtractor(
			BasicJavaDescriptor<X> javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int position, ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getLong( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(CallableStatement statement, int position, ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getLong( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getLong( name ), executionContext.getSession() );
			}
		};
	}
}
