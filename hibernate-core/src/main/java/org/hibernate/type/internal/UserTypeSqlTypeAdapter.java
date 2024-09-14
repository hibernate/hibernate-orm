/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.descriptor.JdbcBindingLogging;
import org.hibernate.type.descriptor.JdbcExtractingLogging;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.UserType;

/**
 * Adapts UserType to the JdbcType contract
 *
 * @author Steve Ebersole
 */
public class UserTypeSqlTypeAdapter<J> implements JdbcType {
	private final UserType<J> userType;
	private final JavaType<J> jtd;

	private final ValueExtractor<J> valueExtractor;
	private final ValueBinder<J> valueBinder;

	public UserTypeSqlTypeAdapter(UserType<J> userType, JavaType<J> jtd, TypeConfiguration typeConfiguration) {
		this.userType = userType;
		this.jtd = jtd;

		this.valueExtractor = new ValueExtractorImpl<>( userType, jtd );
		this.valueBinder = new ValueBinderImpl<>( userType, typeConfiguration );
	}

	@Override
	public String getFriendlyName() {
		return "UserTypeSqlTypeAdapter(" + userType + ")";
	}

	@Override
	public int getJdbcTypeCode() {
		return userType.getSqlType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		assert javaType.getJavaTypeClass() == null
				|| jtd.getJavaTypeClass().isAssignableFrom( javaType.getJavaTypeClass() );
		return (ValueBinder<X>) valueBinder;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		assert javaType.getJavaTypeClass() == null
				|| javaType.getJavaTypeClass().isAssignableFrom( jtd.getJavaTypeClass() );
		return (ValueExtractor<X>) valueExtractor;
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (JavaType<T>) jtd;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		if ( !( userType instanceof EnhancedUserType<?> ) ) {
			throw new HibernateException(
					String.format(
							"Could not create JdbcLiteralFormatter, UserType class [%s] did not implement %s",
							userType.getClass().getName(),
							EnhancedUserType.class.getName()
					)
			);
		}
		final EnhancedUserType<T> type = (EnhancedUserType<T>) userType;
		return (appender, value, dialect, wrapperOptions) -> appender.append( type.toSqlLiteral( value ) );
	}

	private static class ValueExtractorImpl<J> implements ValueExtractor<J> {
		private final UserType<J> userType;
		private final JavaType<J> javaType;

		public ValueExtractorImpl(UserType<J> userType, JavaType<J> javaType) {
			this.userType = userType;
			this.javaType = javaType;
		}

		@Override
		public J extract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
			final J extracted = userType.nullSafeGet( rs, paramIndex, options.getSession(), null );
			logExtracted( paramIndex, extracted );
			return extracted;
		}

		@Override
		public J extract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
			if ( userType instanceof ProcedureParameterExtractionAware ) {
				//noinspection unchecked
				final J extracted = ( (ProcedureParameterExtractionAware<J>) userType ).extract( statement, paramIndex, options.getSession() );
				logExtracted( paramIndex, extracted );
				return extracted;
			}

			throw new UnsupportedOperationException( "UserType does not support reading CallableStatement parameter values: " + userType );
		}

		@Override
		public J extract(CallableStatement statement, String paramName, WrapperOptions options) throws SQLException {
			if ( userType instanceof ProcedureParameterExtractionAware ) {
				//noinspection unchecked
				final J extracted = ( (ProcedureParameterExtractionAware<J>) userType ).extract( statement, paramName, options.getSession() );
				logExtracted( paramName, extracted );
				return extracted;
			}

			throw new UnsupportedOperationException( "UserType does not support reading CallableStatement parameter values: " + userType );
		}

		private void logExtracted(int paramIndex, J extracted) {
			if ( ! JdbcExtractingLogging.LOGGER.isTraceEnabled() ) {
				return;
			}

			if ( extracted == null ) {
				JdbcExtractingLogging.logNullExtracted( paramIndex, userType.getSqlType() );
			}
			else {
				JdbcExtractingLogging.logExtracted( paramIndex, userType.getSqlType(), extracted );
			}
		}

		private void logExtracted(String paramName, J extracted) {
			if ( ! JdbcExtractingLogging.LOGGER.isTraceEnabled() ) {
				return;
			}

			if ( extracted == null ) {
				JdbcExtractingLogging.logNullExtracted( paramName, userType.getSqlType() );
			}
			else {
				JdbcExtractingLogging.logExtracted( paramName, userType.getSqlType(), extracted );
			}
		}
	}

	private static class ValueBinderImpl<J> implements ValueBinder<J> {
		private final UserType<J> userType;

		public ValueBinderImpl(UserType<J> userType, TypeConfiguration typeConfiguration) {
			this.userType = userType;
		}

		@Override
		public void bind(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException {
			if ( JdbcBindingLogging.LOGGER.isTraceEnabled() ) {
				if ( value == null ) {
					JdbcBindingLogging.logNullBinding( index, userType.getSqlType() );
				}
				else {
					JdbcBindingLogging.logBinding( index, userType.getSqlType(), value );
				}
			}
			userType.nullSafeSet( st, value, index, options.getSession() );
		}

		@Override
		public void bind(CallableStatement st, J value, String name, WrapperOptions options) throws SQLException {
			throw new UnsupportedOperationException( "Using UserType for CallableStatement parameter binding not supported" );
		}
	}
}
