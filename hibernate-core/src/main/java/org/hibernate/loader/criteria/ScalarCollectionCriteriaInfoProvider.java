/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
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
