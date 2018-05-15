/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import javax.persistence.TemporalType;

import org.hibernate.sql.AbstractJdbcValueBinder;
import org.hibernate.sql.AbstractJdbcValueExtractor;
import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.TemporalJavaDescriptor;
import org.hibernate.type.descriptor.sql.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#DATE DATE} handling.
 *
 * @author Steve Ebersole
 */
public class DateSqlDescriptor extends AbstractTemplateSqlTypeDescriptor implements TemporalSqlDescriptor {
	public static final DateSqlDescriptor INSTANCE = new DateSqlDescriptor();

	public DateSqlDescriptor() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.DATE;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> TemporalJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (TemporalJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Date.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterTemporal( (TemporalJavaDescriptor) javaTypeDescriptor, TemporalType.DATE );
	}

	@Override
	protected <X> JdbcValueBinder<X> createBinder(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
		return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(
					PreparedStatement st,
					int index, X value,
					ExecutionContext executionContext) throws SQLException {
				final Date date = javaTypeDescriptor.unwrap( value, Date.class, executionContext.getSession() );
				if ( value instanceof Calendar ) {
					st.setDate( index, date, (Calendar) value );
				}
				else {
					st.setDate( index, date );
				}
			}

			@Override
			protected void doBind(
					CallableStatement st,
					String name, X value,
					ExecutionContext executionContext) throws SQLException {
				final Date date = javaTypeDescriptor.unwrap( value, Date.class, executionContext.getSession() );
				if ( value instanceof Calendar ) {
					st.setDate( name, date, (Calendar) value );
				}
				else {
					st.setDate( name, date );
				}
			}
		};
	}

	@Override
	protected <X> JdbcValueExtractor<X> createExtractor(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
		return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int position, ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getDate( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(CallableStatement statement, int position, ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getDate( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getDate( name ), executionContext.getSession() );
			}
		};
	}
}
