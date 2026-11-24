package org.hibernate.dialect.type.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.annotation.Nullable;
import org.hibernate.Internal;
import org.hibernate.TemporalPreservationException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingExpression;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

/// Descriptor which exchanges a [LocalDateTime] with DB2 as text, which the
/// database itself converts to and from the `TIMESTAMP` of the column.
///
/// The DB2 JDBC driver passes a `LocalDateTime` through the default time zone
/// of the JVM however it is exchanged, whether as a [java.sql.Timestamp], as
/// a `LocalDateTime` or as a string which the driver knows to stand for a
/// `TIMESTAMP`. A time which that time zone skipped as clocks were set
/// forward is stored and read as a different time, as is a date in the ten
/// days before 15 October 1582.
///
/// The driver leaves a string alone when the database declares it as a
/// string. So a value is written through a parameter which is cast to
/// `VARCHAR` and then to `TIMESTAMP`, and read from an expression which
/// formats the `TIMESTAMP` as text, both on the server. Neither the default
/// time zone of the JVM nor that of the server plays any part.
///
/// The expressions are applied to the SQL which Hibernate generates. A native
/// query has to do the same for itself: a value of a `TIMESTAMP` column read
/// through this descriptor is rejected, since the driver would have converted
/// it already.
///
/// @author Steve Ebersole
@Internal
public class DB2LocalDateTimeAsTextJdbcType extends LocalDateTimeJdbcType {
	public static final DB2LocalDateTimeAsTextJdbcType INSTANCE = new DB2LocalDateTimeAsTextJdbcType();

	/// The format of the text read from the database, as DB2 names it.
	public static final String READ_FORMAT = "YYYY-MM-DD HH24:MI:SS.FF9";

	private static final DateTimeFormatter WRITE_FORMAT = DateTimeFormatter.ofPattern( "uuuu-MM-dd HH:mm:ss.SSSSSSSSS" );

	@Override
	public void appendWriteExpression(
			String writeExpression,
			@Nullable Size size,
			SqlAppender appender,
			Dialect dialect) {
		// the inner cast has the database declare the parameter as a string,
		// which the driver then sends as it is
		appender.append( "cast(cast(" );
		appender.append( writeExpression );
		appender.append( " as varchar(40)) as timestamp(12))" );
	}

	@Override
	public boolean isWriteExpressionTyped(Dialect dialect) {
		return true;
	}

	@Override
	public Expression wrapTopLevelSelectionExpression(Expression expression) {
		return new SelfRenderingExpression() {
			@Override
			public void renderToSql(
					SqlAppender sqlAppender,
					SqlAstTranslator<?> walker,
					SessionFactoryImplementor sessionFactory) {
				sqlAppender.append( "varchar_format(" );
				expression.accept( walker );
				sqlAppender.append( ",'" );
				sqlAppender.append( READ_FORMAT );
				sqlAppender.append( "')" );
			}

			@Override
			public JdbcMappingContainer getExpressionType() {
				return expression.getExpressionType();
			}
		};
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setString( index, WRITE_FORMAT.format( javaType.unwrap( value, LocalDateTime.class, options ) ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) {
				throw unsupportedInCall( javaType );
			}

			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, Types.VARCHAR );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) {
				throw unsupportedInCall( javaType );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				if ( !isText( rs.getMetaData().getColumnType( paramIndex ) ) ) {
					throw new TemporalPreservationException(
							"Cannot read a value of type '" + javaType.getTypeName() + "' from column "
							+ paramIndex + " of the result, which is not text: on DB2 a LocalDateTime is read"
							+ " as text formatted by the database, as by varchar_format(…,'" + READ_FORMAT + "')"
					);
				}
				final String text = rs.getString( paramIndex );
				return text == null ? null : javaType.wrap( parse( text ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) {
				throw unsupportedInCall( javaType );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) {
				throw unsupportedInCall( javaType );
			}
		};
	}

	private static boolean isText(int columnType) {
		return switch ( columnType ) {
			case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> true;
			default -> false;
		};
	}

	/// Parse text in the [format][#READ_FORMAT] read from the database. DB2
	/// holds the end of a day as the time `24:00:00` of that day, which is
	/// the start of the next day.
	public static LocalDateTime parse(String text) {
		try {
			final var date = LocalDate.parse( text.substring( 0, 10 ) );
			final String time = text.substring( 11 ).trim();
			return time.startsWith( "24:00:00" )
					? date.plusDays( 1 ).atStartOfDay()
					: date.atTime( LocalTime.parse( time ) );
		}
		catch (DateTimeParseException | IndexOutOfBoundsException e) {
			throw new TemporalPreservationException(
					"Cannot read '" + text + "' as a LocalDateTime: on DB2 a LocalDateTime is read as text"
					+ " formatted by the database as '" + READ_FORMAT + "'",
					e
			);
		}
	}

	private static TemporalPreservationException unsupportedInCall(JavaType<?> javaType) {
		return new TemporalPreservationException(
				"Cannot pass a value of type '" + javaType.getTypeName()
				+ "' to or from a stored procedure or function: on DB2 a LocalDateTime is exchanged as text"
				+ " which the database converts, which has not been established for a parameter of a call"
		);
	}
}
