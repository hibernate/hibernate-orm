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
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

import jakarta.persistence.TemporalType;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#TIMESTAMP TIMESTAMP} handling.
 *
 * @author Steve Ebersole
 */
public class TimestampTypeDescriptor implements JdbcTypeDescriptor {
	public static final TimestampTypeDescriptor INSTANCE = new TimestampTypeDescriptor();

	public TimestampTypeDescriptor() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIMESTAMP;
	}

	@Override
	public String getFriendlyName() {
		return "TIMESTAMP";
	}

	@Override
	public String toString() {
		return "TimestampTypeDescriptor";
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Timestamp.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterTemporal( javaTypeDescriptor, TemporalType.TIMESTAMP );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Timestamp timestamp = javaTypeDescriptor.unwrap( value, Timestamp.class, options );
				if ( value instanceof Calendar ) {
					st.setTimestamp( index, timestamp, (Calendar) value );
				}
				else if ( options.getJdbcTimeZone() != null ) {
					st.setTimestamp( index, timestamp, Calendar.getInstance( options.getJdbcTimeZone() ) );
				}
				else {
					st.setTimestamp( index, timestamp );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Timestamp timestamp = javaTypeDescriptor.unwrap( value, Timestamp.class, options );
				if ( value instanceof Calendar ) {
					st.setTimestamp( name, timestamp, (Calendar) value );
				}
				else if ( options.getJdbcTimeZone() != null ) {
					st.setTimestamp( name, timestamp, Calendar.getInstance( options.getJdbcTimeZone() ) );
				}
				else {
					st.setTimestamp( name, timestamp );
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
					javaTypeDescriptor.wrap( rs.getTimestamp( paramIndex, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
					javaTypeDescriptor.wrap( rs.getTimestamp( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						javaTypeDescriptor.wrap( statement.getTimestamp( index, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						javaTypeDescriptor.wrap( statement.getTimestamp( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						javaTypeDescriptor.wrap( statement.getTimestamp( name, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						javaTypeDescriptor.wrap( statement.getTimestamp( name ), options );
			}
		};
	}
}
