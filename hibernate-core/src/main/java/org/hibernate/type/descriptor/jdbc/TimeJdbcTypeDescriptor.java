/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.util.Calendar;

import jakarta.persistence.TemporalType;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#TIME TIME} handling.
 *
 * @author Steve Ebersole
 */
public class TimeJdbcTypeDescriptor implements JdbcTypeDescriptor {
	public static final TimeJdbcTypeDescriptor INSTANCE = new TimeJdbcTypeDescriptor();

	public TimeJdbcTypeDescriptor() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIME;
	}

	@Override
	public String getFriendlyName() {
		return "TIME";
	}

	@Override
	public String toString() {
		return "TimeTypeDescriptor";
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> BasicJavaTypeDescriptor<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return (BasicJavaTypeDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Time.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterTemporal( javaTypeDescriptor, TemporalType.TIME );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Time time = javaTypeDescriptor.unwrap( value, Time.class, options );
				if ( value instanceof Calendar ) {
					st.setTime( index, time, (Calendar) value );
				}
				else if ( options.getJdbcTimeZone() != null ) {
					st.setTime( index, time, Calendar.getInstance( options.getJdbcTimeZone() ) );
				}
				else {
					st.setTime( index, time );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Time time = javaTypeDescriptor.unwrap( value, Time.class, options );
				if ( value instanceof Calendar ) {
					st.setTime( name, time, (Calendar) value );
				}
				else if ( options.getJdbcTimeZone() != null ) {
					st.setTime( name, time, Calendar.getInstance( options.getJdbcTimeZone() ) );
				}
				else {
					st.setTime( name, time );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						javaTypeDescriptor.wrap( rs.getTime( paramIndex, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						javaTypeDescriptor.wrap( rs.getTime( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						javaTypeDescriptor.wrap( statement.getTime( index, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						javaTypeDescriptor.wrap( statement.getTime( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						javaTypeDescriptor.wrap( statement.getTime( name, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						javaTypeDescriptor.wrap( statement.getTime( name ), options );
			}
		};
	}
}
