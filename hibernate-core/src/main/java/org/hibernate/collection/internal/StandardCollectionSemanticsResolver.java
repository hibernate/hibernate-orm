/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.internal;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.CollectionSemanticsResolver;
import org.hibernate.mapping.Collection;

/**
 * Standard implementation of CollectionSemanticsResolver
 *
 * @author Steve Ebersole
 */
public class StandardCollectionSemanticsResolver implements CollectionSemanticsResolver {
	/**
	 * Singleton access
	 */
	public static final StandardCollectionSemanticsResolver INSTANCE = new StandardCollectionSemanticsResolver();

	@Override
	public CollectionSemantics resolveRepresentation(Collection bootDescriptor) {
		return bootDescriptor.getCollectionSemantics();
	}
}
