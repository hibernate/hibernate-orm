/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
import org.hibernate.type.descriptor.java.BasicJavaType;
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
	private final BasicJavaType<J> jtd;

	private final ValueExtractor<J> valueExtractor;
	private final ValueBinder<J> valueBinder;

	public UserTypeSqlTypeAdapter(UserType<J> userType, BasicJavaType<J> jtd, TypeConfiguration typeConfiguration) {
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
		return userType.sqlTypes()[0];
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> ValueBinder<X> getBinder(JavaType<X> javaTypeDescriptor) {
		assert jtd.getJavaTypeClass().isAssignableFrom( javaTypeDescriptor.getJavaTypeClass() );
		return (ValueBinder<X>) valueBinder;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		assert javaTypeDescriptor.getJavaTypeClass().isAssignableFrom( jtd.getJavaTypeClass() );
		return (ValueExtractor<X>) valueExtractor;
	}

	@Override
	public <T> BasicJavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (BasicJavaType<T>) jtd;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
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

		public ValueExtractorImpl(UserType<J> userType, BasicJavaType<J> javaType) {
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
			if ( ! JdbcExtractingLogging.TRACE_ENABLED ) {
				return;
			}

			if ( extracted == null ) {
				JdbcExtractingLogging.logNullExtracted( paramIndex, userType.sqlTypes()[0] );
			}
			else {
				JdbcExtractingLogging.logExtracted( paramIndex, userType.sqlTypes()[0], extracted );
			}
		}

		private void logExtracted(String paramName, J extracted) {
			if ( ! JdbcExtractingLogging.TRACE_ENABLED ) {
				return;
			}

			if ( extracted == null ) {
				JdbcExtractingLogging.logNullExtracted( paramName, userType.sqlTypes()[0] );
			}
			else {
				JdbcExtractingLogging.logExtracted( paramName, userType.sqlTypes()[0], extracted );
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
			if ( JdbcBindingLogging.TRACE_ENABLED ) {
				if ( value == null ) {
					JdbcBindingLogging.logNullBinding( index, userType.sqlTypes()[ 0 ] );
				}
				else {
					JdbcBindingLogging.logBinding( index, userType.sqlTypes()[ 0 ], value );
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
