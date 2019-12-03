/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public class CollectionLoaderNamedQuery implements CollectionLoader {
	private final String loaderQueryName;
	private final CollectionPersister persister;
	private final PluralAttributeMapping attributeMapping;

	public CollectionLoaderNamedQuery(
			String loaderQueryName,
			CollectionPersister persister,
			PluralAttributeMapping attributeMapping) {
		this.loaderQueryName = loaderQueryName;
		this.persister = persister;
		this.attributeMapping = attributeMapping;
	}

	@Override
	public PluralAttributeMapping getLoadable() {
		return attributeMapping;
	}

	@Override
	public PersistentCollection load(Object key, SharedSessionContractImplementor session) {
		return null;
	}
}
