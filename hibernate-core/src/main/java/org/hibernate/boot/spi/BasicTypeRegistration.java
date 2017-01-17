/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.HibernateException;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.BasicTypeRegistry;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

/**
 * @author Steve Ebersole
 */
public class BasicTypeRegistration {
	private final BasicType basicType;
	private final BasicTypeRegistry.Key registryKey;

	public BasicTypeRegistration(BasicType basicType, BasicTypeRegistry.Key registryKey) {
		this.basicType = basicType;
		this.registryKey = registryKey;
	}

	public BasicTypeRegistration(UserType type, String[] keys) {
		throw new HibernateException( "BasicTypeRegistration does not support UserType" );
	}

	public BasicTypeRegistration(CompositeUserType type, String[] keys) {
		throw new HibernateException( "BasicTypeRegistration does not support CompositeUserType" );
	}

	public BasicType getBasicType() {
		return basicType;
	}

	public BasicTypeRegistry.Key getRegistryKey() {
		return registryKey;
	}
}
