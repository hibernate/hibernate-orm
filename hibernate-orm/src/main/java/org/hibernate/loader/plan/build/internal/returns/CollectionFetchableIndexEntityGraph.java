/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.plan.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan.spi.CollectionFetchableIndex;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.persister.walking.spi.AttributeDefinition;

/**
 * @author Steve Ebersole
 */
public class CollectionFetchableIndexEntityGraph extends AbstractEntityReference implements CollectionFetchableIndex {
	private final CollectionReference collectionReference;

	public CollectionFetchableIndexEntityGraph(
			CollectionReference collectionReference,
			ExpandingEntityQuerySpace entityQuerySpace) {
		super(
				entityQuerySpace,
				collectionReference.getPropertyPath().append( "<index>" )
		);

		this.collectionReference = collectionReference;
	}

	@Override
	public CollectionReference getCollectionReference() {
		return collectionReference;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
	}
}
