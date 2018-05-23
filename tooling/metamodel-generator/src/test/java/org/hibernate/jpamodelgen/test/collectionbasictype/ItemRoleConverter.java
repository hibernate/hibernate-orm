/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.collectionbasictype;

import javax.persistence.AttributeConverter;

/**
 * @author Chris Cranford
 */
public class ItemRoleConverter implements AttributeConverter<Role, String> {
	@Override
	public String convertToDatabaseColumn(Role role) {
		return role == null ? null : role.getType();
	}

	@Override
	public Role convertToEntityAttribute(String s) {
		if ( s != null ) {
			if ( RoleInternal.TYPE.equals( s ) ) {
				return new RoleInternal();
			}
			else if ( RoleExternal.TYPE.equals( s ) ) {
				return new RoleExternal();
			}
		}
		return null;
	}
}
