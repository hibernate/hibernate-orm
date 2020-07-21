/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.metamodel.mapping.EntityIdentifierMapping;

/**
 * @author Andrea Boriero
 */
public class EntityIdentifierNavigablePath extends NavigablePath {

	public EntityIdentifierNavigablePath(NavigablePath parent) {
		super( parent, EntityIdentifierMapping.ROLE_LOCAL_NAME );
	}

	@Override
	public String getLocalName() {
		return EntityIdentifierMapping.ROLE_LOCAL_NAME;
	}
}
