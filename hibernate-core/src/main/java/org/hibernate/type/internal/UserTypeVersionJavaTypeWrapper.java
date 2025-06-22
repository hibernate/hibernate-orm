/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.VersionJavaType;
import org.hibernate.usertype.UserVersionType;

/**
 *
 * @author Christian Beikov
 */
public class UserTypeVersionJavaTypeWrapper<J> extends UserTypeJavaTypeWrapper<J> implements VersionJavaType<J> {

	public UserTypeVersionJavaTypeWrapper(UserVersionType<J> userType, CustomType<J> customType) {
		super( userType, customType );
	}

	@Override
	public J seed(
			Long length,
			Integer precision,
			Integer scale, SharedSessionContractImplementor session) {
		return ( (UserVersionType<J>) userType ).seed( session );
	}

	@Override
	public J next(
			J current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return ( (UserVersionType<J>) userType ).next( current, session );
	}
}
