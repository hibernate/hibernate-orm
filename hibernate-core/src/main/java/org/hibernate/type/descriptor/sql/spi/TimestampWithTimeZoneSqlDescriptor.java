/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

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

import javax.persistence.TemporalType;
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
public class TimestampWithTimeZoneSqlDescriptor extends AbstractTemplateSqlTypeDescriptor implements TemporalSqlDescriptor {
	public static final TimestampWithTimeZoneSqlDescriptor INSTANCE = new TimestampWithTimeZoneSqlDescriptor();

	public TimestampWithTimeZoneSqlDescriptor() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIMESTAMP_WITH_TIMEZONE;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> TemporalJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (TemporalJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( OffsetDateTime.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterTemporal( (TemporalJavaDescriptor) javaTypeDescriptor, TemporalType.TIMESTAMP );
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
				try {
					final OffsetDateTime dateTime = javaTypeDescriptor.unwrap( value, OffsetDateTime.class, executionContext.getSession() );
					// supposed to be supported in JDBC 4.2
					st.setObject( index, dateTime, Types.TIMESTAMP_WITH_TIMEZONE );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
					final Timestamp timestamp = javaTypeDescriptor.unwrap( value, Timestamp.class, executionContext.getSession() );
					st.setTimestamp( index, timestamp );
				}
			}

			@Override
			protected void doBind(
					CallableStatement st,
					String name,
					X value,
					ExecutionContext executionContext)
					throws SQLException {
				try {
					final OffsetDateTime dateTime = javaTypeDescriptor.unwrap( value, OffsetDateTime.class, executionContext.getSession() );
					// supposed to be supported in JDBC 4.2
					st.setObject( name, dateTime, Types.TIMESTAMP_WITH_TIMEZONE );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
					final Timestamp timestamp = javaTypeDescriptor.unwrap( value, Timestamp.class, executionContext.getSession() );
					st.setTimestamp( name, timestamp );
				}
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
				try {
					// supposed to be supported in JDBC 4.2
					return javaTypeDescriptor.wrap( rs.getObject( position, OffsetDateTime.class ), executionContext.getSession() );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
					return javaTypeDescriptor.wrap( rs.getTimestamp( position ), executionContext.getSession() );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int position, ExecutionContext executionContext) throws SQLException {
				try {
					// supposed to be supported in JDBC 4.2
					return javaTypeDescriptor.wrap( statement.getObject( position, OffsetDateTime.class ), executionContext.getSession() );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
					return javaTypeDescriptor.wrap( statement.getTimestamp( position ), executionContext.getSession() );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, ExecutionContext executionContext) throws SQLException {
				try {
					// supposed to be supported in JDBC 4.2
					return javaTypeDescriptor.wrap( statement.getObject( name, OffsetDateTime.class ), executionContext.getSession() );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
					return javaTypeDescriptor.wrap( statement.getTimestamp( name ), executionContext.getSession() );
				}
			}
		};
	}
}
