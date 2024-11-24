/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;

/**
 * Hibernate "wraps" a java collection in an instance of PersistentCollection. Envers uses custom collection
 * wrappers (ListProxy, SetProxy, etc). All of them need to extend LazyInitializable, so the
 * Hibernate.isInitialized method can check if the collection is initialized or not.
 * 
 * @author Fabricio Gregorio
 */
@Incubating
public interface LazyInitializable {

	/**
	 * Is this instance initialized?
	 *
	 * @return Was this collection initialized? Or is its data still not (fully) loaded?
	 */
	boolean wasInitialized();
	
	/**
	 * To be called internally by the session, forcing immediate initialization.
	 */
	void forceInitialization();

}
