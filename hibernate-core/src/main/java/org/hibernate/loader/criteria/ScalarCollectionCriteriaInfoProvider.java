/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.criteria;

import java.io.Serializable;

import org.hibernate.hql.internal.ast.util.SessionFactoryHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.Type;

/**
 * @author David Mansfield
 */

class ScalarCollectionCriteriaInfoProvider implements CriteriaInfoProvider {
	private final String role;
	private final QueryableCollection persister;
	private final SessionFactoryHelper helper;

	ScalarCollectionCriteriaInfoProvider(SessionFactoryHelper helper, String role) {
		this.role = role;
		this.helper = helper;
		this.persister = helper.requireQueryableCollection( role );
	}

	@Override
	public String getName() {
		return role;
	}

	@Override
	public Serializable[] getSpaces() {
		return persister.getCollectionSpaces();
	}

	@Override
	public PropertyMapping getPropertyMapping() {
		return helper.getCollectionPropertyMapping( role );
	}

	@Override
	public Type getType(String relativePath) {
		//not sure what things are going to be passed here, how about 'id', maybe 'index' or 'key' or 'elements' ???
		// todo: wtf!
		return getPropertyMapping().toType( relativePath );
	}

}
