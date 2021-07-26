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

import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

/**
 * Adapts UserType to the JdbcTypeDescriptor contract
 *
 * @author Steve Ebersole
 */
public class UserTypeSqlTypeAdapter<J> implements JdbcTypeDescriptor {
	private final UserType<J> userType;
	private final BasicJavaDescriptor<J> jtd;

	private final ValueExtractor<J> valueExtractor;
	private final ValueBinder<J> valueBinder;

	public UserTypeSqlTypeAdapter(UserType<J> userType, BasicJavaDescriptor<J> jtd) {
		this.userType = userType;
		this.jtd = jtd;

		this.valueExtractor = new ValueExtractorImpl<>( userType );
		this.valueBinder = new ValueBinderImpl<>( userType );
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
	public boolean canBeRemapped() {
		return false;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		assert jtd.getJavaTypeClass().isAssignableFrom( javaTypeDescriptor.getJavaTypeClass() );
		return (ValueBinder<X>) valueBinder;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		assert javaTypeDescriptor.getJavaTypeClass().isAssignableFrom( jtd.getJavaTypeClass() );
		return (ValueExtractor<X>) valueExtractor;
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (BasicJavaDescriptor<T>) jtd;
	}

	private static class ValueExtractorImpl<J> implements ValueExtractor<J> {
		private final UserType<J> userType;

		public ValueExtractorImpl(UserType<J> userType) {
			this.userType = userType;
		}

		@Override
		public J extract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
			return userType.nullSafeGet( rs, paramIndex, options.getSession(), null );
		}

		@Override
		public J extract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
			if ( userType instanceof ProcedureParameterExtractionAware ) {
				//noinspection unchecked
				return ( (ProcedureParameterExtractionAware<J>) userType ).extract( statement, paramIndex, options.getSession() );
			}

			throw new UnsupportedOperationException( "UserType does not support reading CallableStatement parameter values: " + userType );
		}

		@Override
		public J extract(CallableStatement statement, String paramName, WrapperOptions options) throws SQLException {
			if ( userType instanceof ProcedureParameterExtractionAware ) {
				//noinspection unchecked
				return ( (ProcedureParameterExtractionAware<J>) userType ).extract( statement, paramName, options.getSession() );
			}

			throw new UnsupportedOperationException( "UserType does not support reading CallableStatement parameter values: " + userType );
		}
	}

	private static class ValueBinderImpl<J> implements ValueBinder<J> {
		private final UserType<J> userType;

		public ValueBinderImpl(UserType<J> userType) {
			this.userType = userType;
		}

		@Override
		public void bind(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException {
			userType.nullSafeSet( st, value, index, options.getSession() );
		}

		@Override
		public void bind(CallableStatement st, J value, String name, WrapperOptions options) throws SQLException {
			throw new UnsupportedOperationException( "Using UserType for CallableStatement parameter binding not supported" );
		}
	}
}
