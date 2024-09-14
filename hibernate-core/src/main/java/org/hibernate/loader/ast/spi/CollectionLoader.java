/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;

/**
 * A loader (initialization) for collections
 *
 * @author Steve Ebersole
 */
public interface CollectionLoader extends Loader {
	@Override
	PluralAttributeMapping getLoadable();

	/**
	 * Load a collection by its key (not necessarily the same as its owner's PK).
	 */
	PersistentCollection<?> load(Object key, SharedSessionContractImplementor session);

	//TODO support 'readOnly' collection loading

}
