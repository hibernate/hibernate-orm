/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.criteria;

import java.io.Serializable;

import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.Type;

/**
 * @author David Mansfield
 */

class EntityCriteriaInfoProvider implements CriteriaInfoProvider {
	private final Queryable persister;

	EntityCriteriaInfoProvider(Queryable persister) {
		this.persister = persister;
	}
	@Override
	public String getName() {
		return persister.getEntityName();
	}
	@Override
	public Serializable[] getSpaces() {
		return persister.getQuerySpaces();
	}
	@Override
	public PropertyMapping getPropertyMapping() {
		return persister;
	}
	@Override
	public Type getType(String relativePath) {
		return persister.toType( relativePath );
	}
}
