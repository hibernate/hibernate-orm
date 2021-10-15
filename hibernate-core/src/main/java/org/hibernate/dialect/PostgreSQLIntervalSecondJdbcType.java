/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;

import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * @author Christian Beikov
 */
public class PostgreSQLIntervalSecondJdbcType implements AdjustableJdbcType {

	public static final PostgreSQLIntervalSecondJdbcType INSTANCE = new PostgreSQLIntervalSecondJdbcType();
	private static final Constructor<Object> PG_INTERVAL_CONSTRUCTOR;

	static {
		Constructor<Object> constructor;
		try {
			final Class<?> pgIntervalClass = ReflectHelper.classForName(
					"org.postgresql.util.PGInterval",
					PostgreSQLIntervalSecondJdbcType.class
			);
			constructor = (Constructor<Object>) pgIntervalClass.getConstructor(
					int.class,
					int.class,
					int.class,
					int.class,
					int.class,
					double.class
			);
		}
		catch (Exception e) {
			throw new RuntimeException( "Could not initialize PostgreSQLPGObjectJdbcType", e );
		}
		PG_INTERVAL_CONSTRUCTOR = constructor;
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.OTHER;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.INTERVAL_SECOND;
	}

	@Override
	public JdbcType resolveIndicatedType(JdbcTypeDescriptorIndicators indicators, JavaType<?> domainJtd) {
		// The default scale is 9
		if ( indicators.getColumnScale() == JdbcTypeDescriptorIndicators.NO_COLUMN_SCALE || indicators.getColumnScale() > 6 ) {
			return indicators.getTypeConfiguration().getJdbcTypeDescriptorRegistry().getDescriptor( SqlTypes.NUMERIC );
		}
		return this;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return (appender, value, dialect, wrapperOptions) -> dialect.appendIntervalLiteral(
				appender,
				javaTypeDescriptor.unwrap(
						value,
						Duration.class,
						wrapperOptions
				)
		);
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final Duration duration = getJavaTypeDescriptor().unwrap( value, Duration.class, options );
				st.setObject( index, constructInterval( duration ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Duration duration = getJavaTypeDescriptor().unwrap( value, Duration.class, options );
				st.setObject( name, constructInterval( duration ) );
			}

			private Object constructInterval(Duration d) {
				long secondsLong = d.getSeconds();
				long minutesLong = secondsLong / 60;
				long hoursLong = minutesLong / 60;
				long daysLong = hoursLong / 24;
				int days = Math.toIntExact( daysLong );
				int hours = (int) ( hoursLong - daysLong * 24 );
				int minutes = (int) ( minutesLong - hoursLong * 60 );
				double seconds = ( (double) ( secondsLong - minutesLong * 60 ) )
						+ ( (double) d.getNano() ) / 1_000_000_000d;

				try {
					return PG_INTERVAL_CONSTRUCTOR.newInstance(
							0,// years
							0, // months
							days,
							hours,
							minutes,
							seconds
					);
				}
				catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
					throw new IllegalArgumentException( e );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaTypeDescriptor().wrap( rs.getString( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaTypeDescriptor().wrap( statement.getString( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaTypeDescriptor().wrap( statement.getString( name ), options );
			}
		};
	}
}
