/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

/**
 * @author Steve Ebersole
 */
public class BasicTypeRegistration {
	private final BasicType basicType;
	private final String[] registrationKeys;

	public BasicTypeRegistration(BasicType basicType) {
		this( basicType, basicType.getRegistrationKeys() );
	}

	public BasicTypeRegistration(BasicType basicType, String[] registrationKeys) {
		this.basicType = basicType;
		this.registrationKeys = registrationKeys;
	}

	public BasicTypeRegistration(UserType type, String[] keys, TypeConfiguration typeConfiguration) {
		this( new CustomType( type, keys, typeConfiguration ), keys );
	}

	public BasicType getBasicType() {
		return basicType;
	}

	public String[] getRegistrationKeys() {
		return registrationKeys;
	}
}
