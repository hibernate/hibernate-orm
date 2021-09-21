/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;

/**
 * Descriptor for {@link Types#TIMESTAMP_WITH_TIMEZONE TIMESTAMP_WITH_TIMEZONE} handling.
 *
 * @author Gavin King
 */
public class TimestampWithTimeZoneDescriptor implements JdbcTypeDescriptor {
	public static final TimestampWithTimeZoneDescriptor INSTANCE = new TimestampWithTimeZoneDescriptor();

	public TimestampWithTimeZoneDescriptor() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIMESTAMP_WITH_TIMEZONE;
	}

	@Override
	public String getFriendlyName() {
		return "TIMESTAMP_WITH_TIMEZONE";
	}

	@Override
	public String toString() {
		return "TimestampWithTimeZoneDescriptor";
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( OffsetDateTime.class );
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
			protected void doBind(
					PreparedStatement st,
					X value,
					int index,
					WrapperOptions wrapperOptions) throws SQLException {
				try {
					final OffsetDateTime dateTime = javaTypeDescriptor.unwrap( value, OffsetDateTime.class, wrapperOptions );
					// supposed to be supported in JDBC 4.2
					st.setObject( index, dateTime, Types.TIMESTAMP_WITH_TIMEZONE );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
					final Timestamp timestamp = javaTypeDescriptor.unwrap( value, Timestamp.class, wrapperOptions );
					st.setTimestamp( index, timestamp );
				}
			}

			@Override
			protected void doBind(
					CallableStatement st,
					X value,
					String name,
					WrapperOptions wrapperOptions)
					throws SQLException {
				try {
					final OffsetDateTime dateTime = javaTypeDescriptor.unwrap( value, OffsetDateTime.class, wrapperOptions );
					// supposed to be supported in JDBC 4.2
					st.setObject( name, dateTime, Types.TIMESTAMP_WITH_TIMEZONE );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
					final Timestamp timestamp = javaTypeDescriptor.unwrap( value, Timestamp.class, wrapperOptions );
					st.setTimestamp( name, timestamp );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int position, WrapperOptions wrapperOptions) throws SQLException {
				try {
					// supposed to be supported in JDBC 4.2
					return javaTypeDescriptor.wrap( rs.getObject( position, OffsetDateTime.class ), wrapperOptions );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
					return javaTypeDescriptor.wrap( rs.getTimestamp( position ), wrapperOptions );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int position, WrapperOptions wrapperOptions) throws SQLException {
				try {
					// supposed to be supported in JDBC 4.2
					return javaTypeDescriptor.wrap( statement.getObject( position, OffsetDateTime.class ), wrapperOptions );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
					return javaTypeDescriptor.wrap( statement.getTimestamp( position ), wrapperOptions );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions wrapperOptions) throws SQLException {
				try {
					// supposed to be supported in JDBC 4.2
					return javaTypeDescriptor.wrap( statement.getObject( name, OffsetDateTime.class ), wrapperOptions );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
					return javaTypeDescriptor.wrap( statement.getTimestamp( name ), wrapperOptions );
				}
			}
		};
	}
}
