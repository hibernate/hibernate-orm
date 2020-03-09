/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;

/**
 * @author Steve Ebersole
 */
public class EagerCollectionAssembler extends AbstractCollectionAssembler {

	public EagerCollectionAssembler(
			PluralAttributeMapping fetchedMapping,
			CollectionInitializer initializer) {
		super( fetchedMapping, initializer );
	}
}
