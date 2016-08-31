/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Descriptor for {@link Types#TIMESTAMP TIMESTAMP} handling (using UTC as default timezone).
 *
 * @author Steve Ebersole
 */
public class UtcTimestampTypeDescriptor implements SqlTypeDescriptor {
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	public static final UtcTimestampTypeDescriptor INSTANCE = new UtcTimestampTypeDescriptor();

	public UtcTimestampTypeDescriptor() {
	}

	@Override
	public int getSqlType() {
		return Types.TIMESTAMP;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
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
				else {
					st.setTimestamp( index, timestamp, Calendar.getInstance(UTC) );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Timestamp timestamp = javaTypeDescriptor.unwrap( value, Timestamp.class, options );
				if ( value instanceof Calendar ) {
					st.setTimestamp( name, timestamp, (Calendar) value );
				}
				else {
					st.setTimestamp( name, timestamp, Calendar.getInstance(UTC) );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getTimestamp( name, Calendar.getInstance(UTC) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getTimestamp( index, Calendar.getInstance(UTC) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getTimestamp( name, Calendar.getInstance(UTC) ), options );
			}
		};
	}
}
