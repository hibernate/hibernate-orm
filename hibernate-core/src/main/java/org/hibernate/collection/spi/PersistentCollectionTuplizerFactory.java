/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.spi;

/**
 * Factory for {@link PersistentCollectionTuplizer} instances
 *
 * @author Steve Ebersole
 */
public interface PersistentCollectionTuplizerFactory {

	// todo (6.0) : what about passing PersistentCollectionDescriptor.CollectionClassification, rather than the "collection type"?

	// todo (6.0) : any benefit to passing along the NavigableRole?
	//		- the idea being that the factory impl could be sensitive
	//			to each role, possibly through configuration via settings

	// todo (6.0) : relatedly ^^, anything else we should pass along?

	PersistentCollectionTuplizer getImplicitTuplizer(Class collectionJavaType);
}
