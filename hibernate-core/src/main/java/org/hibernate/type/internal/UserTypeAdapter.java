/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.usertype.UserType;

/**
 * @author Steve Ebersole
 */
public class UserTypeAdapter<J> implements BasicType<J> {
	private final UserType userType;
	private final String name;

	private final BasicJavaDescriptor<J> jtd;

	private final SqlExpressableType sqlExpressableType;


	private UserTypeAdapter(
			UserType userType,
			String name,
			BasicJavaDescriptor<J> jtd,
			SqlTypeDescriptor std) {
		this.userType = userType;
		this.name = name;
		this.jtd = jtd;
		this.sqlExpressableType = new UserTypeSqlExpressableTypeAdapter( jtd, std, userType );
	}


	@SuppressWarnings("unchecked")
	public UserTypeAdapter(
			UserType userType,
			String name,
			ResolutionContext resolutionContext) {
		this(
				userType,
				name,
				(BasicJavaDescriptor) resolutionContext.getBootstrapContext()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( userType.returnedClass() ),
				resolutionContext.getBootstrapContext()
						.getTypeConfiguration()
						.getSqlTypeDescriptorRegistry()
						.getDescriptor( userType.sqlTypeCode() )
		);
	}

	public UserTypeAdapter(
			UserType userType,
			String name,
			BasicJavaDescriptor explicitJtd,
			SqlTypeDescriptor explicitStd,
			ResolutionContext resolutionContext) {
		this(
				userType,
				name,
				determineJavaTypeDescriptor(
						userType,
						explicitJtd,
						resolutionContext
				),
				explicitStd != null
						? explicitStd
						: resolutionContext.getBootstrapContext()
						.getTypeConfiguration()
						.getSqlTypeDescriptorRegistry()
						.getDescriptor( userType.sqlTypeCode() )
		);
	}

	@SuppressWarnings("unchecked")
	private static <J> BasicJavaDescriptor<J> determineJavaTypeDescriptor(
			UserType userType,
			BasicJavaDescriptor explicitJtd,
			ResolutionContext resolutionContext) {
		if ( explicitJtd != null ) {
			return explicitJtd;
		}

		return determineJavaTypeDescriptor( userType, resolutionContext );
	}

	@SuppressWarnings("unchecked")
	private static <J> BasicJavaDescriptor<J> determineJavaTypeDescriptor(
			UserType userType,
			ResolutionContext resolutionContext) {
		final Class<?> domainJavaType = userType.returnedClass();
		return (BasicJavaDescriptor) resolutionContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( domainJavaType );
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return jtd;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return getSqlExpressableType().getSqlTypeDescriptor();
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return sqlExpressableType;
	}

	@Override
	public String toString() {
		return "UserTypeAdapter(" + userType + " : " + name + ")";
	}
}
