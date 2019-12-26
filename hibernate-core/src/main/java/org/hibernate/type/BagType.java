/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.HibernateException;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

public class BagType extends CollectionType {

	/**
	 * @deprecated Use the other constructor
	 */
	@Deprecated
	public BagType(TypeFactory.TypeScope typeScope, String role, String propertyRef) {
		this( role, propertyRef );
	}

	public BagType(String role, String propertyRef) {
		super( role, propertyRef );
	}

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Serializable key)
	throws HibernateException {
		return new PersistentBag( session );
	}

	@Override
	public Class getReturnedClass() {
		return java.util.Collection.class;
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentBag( session, (Collection) collection );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 ? new ArrayList() : new ArrayList( anticipatedSize + 1 );
	}

}
