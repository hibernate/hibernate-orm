/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;

import org.hibernate.HibernateException;
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
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * @author Christian Beikov
 */
public class PostgreSQLIntervalSecondJdbcType implements AdjustableJdbcType {

	public static final PostgreSQLIntervalSecondJdbcType INSTANCE = new PostgreSQLIntervalSecondJdbcType();
	private static final Class<?> PG_INTERVAL_CLASS;
	private static final Constructor<Object> PG_INTERVAL_CONSTRUCTOR;
	private static final Method PG_INTERVAL_GET_DAYS;
	private static final Method PG_INTERVAL_GET_HOURS;
	private static final Method PG_INTERVAL_GET_MINUTES;
	private static final Method PG_INTERVAL_GET_SECONDS;
	private static final Method PG_INTERVAL_GET_MICRO_SECONDS;
	private static final long SECONDS_PER_DAY = 86400;
	private static final long SECONDS_PER_HOUR = 3600;
	private static final long SECONDS_PER_MINUTE = 60;

	static {
		Constructor<Object> constructor;
		Class<?> pgIntervalClass;
		Method pgIntervalGetDays;
		Method pgIntervalGetHours;
		Method pgIntervalGetMinutes;
		Method pgIntervalGetSeconds;
		Method pgIntervalGetMicroSeconds;
		try {
			pgIntervalClass = ReflectHelper.classForName(
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
			pgIntervalGetDays = pgIntervalClass.getDeclaredMethod( "getDays" );
			pgIntervalGetHours = pgIntervalClass.getDeclaredMethod( "getHours" );
			pgIntervalGetMinutes = pgIntervalClass.getDeclaredMethod( "getMinutes" );
			pgIntervalGetSeconds = pgIntervalClass.getDeclaredMethod( "getWholeSeconds" );
			pgIntervalGetMicroSeconds = pgIntervalClass.getDeclaredMethod( "getMicroSeconds" );
		}
		catch (Exception e) {
			throw new RuntimeException( "Could not initialize PostgreSQLPGObjectJdbcType", e );
		}
		PG_INTERVAL_CLASS = pgIntervalClass;
		PG_INTERVAL_CONSTRUCTOR = constructor;
		PG_INTERVAL_GET_DAYS = pgIntervalGetDays;
		PG_INTERVAL_GET_HOURS = pgIntervalGetHours;
		PG_INTERVAL_GET_MINUTES = pgIntervalGetMinutes;
		PG_INTERVAL_GET_SECONDS = pgIntervalGetSeconds;
		PG_INTERVAL_GET_MICRO_SECONDS = pgIntervalGetMicroSeconds;
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
	public JdbcType resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<?> domainJtd) {
		// The default scale is 9
		if ( indicators.getColumnScale() == JdbcTypeIndicators.NO_COLUMN_SCALE || indicators.getColumnScale() > 6 ) {
			return indicators.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.NUMERIC );
		}
		return this;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return (appender, value, dialect, wrapperOptions) -> dialect.appendIntervalLiteral(
				appender,
				javaType.unwrap(
						value,
						Duration.class,
						wrapperOptions
				)
		);
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final Duration duration = getJavaType().unwrap( value, Duration.class, options );
				st.setObject( index, constructInterval( duration ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Duration duration = getJavaType().unwrap( value, Duration.class, options );
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
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( getValue( rs.getObject( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( getValue( statement.getObject( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaType().wrap( getValue( statement.getObject( name ) ), options );
			}

			private Object getValue(Object value) {
				if ( PG_INTERVAL_CLASS.isInstance( value ) ) {
					try {
						final long seconds = (int) PG_INTERVAL_GET_SECONDS.invoke( value )
								+ SECONDS_PER_DAY * (int) PG_INTERVAL_GET_DAYS.invoke( value )
								+ SECONDS_PER_HOUR * (int) PG_INTERVAL_GET_HOURS.invoke( value )
								+ SECONDS_PER_MINUTE * (int) PG_INTERVAL_GET_MINUTES.invoke( value );
						final long nanos = 1000L * (int) PG_INTERVAL_GET_MICRO_SECONDS.invoke( value );

						return Duration.ofSeconds( seconds, nanos );
					}
					catch (Exception e) {
						throw new HibernateException( "Couldn't create Duration from interval", e );
					}
				}
				return value;
			}
		};
	}
}
