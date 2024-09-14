/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.VersionJavaType;
import org.hibernate.usertype.UserVersionType;

/**
 *
 * @author Christian Beikov
 */
public class UserTypeVersionJavaTypeWrapper<J> extends UserTypeJavaTypeWrapper<J> implements VersionJavaType<J> {

	public UserTypeVersionJavaTypeWrapper(UserVersionType<J> userType) {
		super( userType );
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
