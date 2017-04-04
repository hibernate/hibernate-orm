/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy;

import java.util.Set;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SetProxy<U> extends CollectionProxy<U, Set<U>> implements Set<U> {
	private static final long serialVersionUID = 131464133074137701L;

	public SetProxy() {
	}

	public SetProxy(org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor<Set<U>> initializor) {
		super( initializor );
	}
}
