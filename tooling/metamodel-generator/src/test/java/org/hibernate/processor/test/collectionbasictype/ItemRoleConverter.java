/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.collectionbasictype;

import jakarta.persistence.AttributeConverter;

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
