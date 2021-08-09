/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import org.hibernate.MappingException;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.CollectionSemanticsResolver;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.Set;

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
