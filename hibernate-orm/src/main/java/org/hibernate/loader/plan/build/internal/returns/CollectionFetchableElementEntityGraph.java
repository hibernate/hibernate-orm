/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.plan.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan.spi.CollectionFetchableElement;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.persister.walking.spi.AttributeDefinition;

/**
 * @author Steve Ebersole
 */
public class CollectionFetchableElementEntityGraph extends AbstractEntityReference implements CollectionFetchableElement {
	private final CollectionReference collectionReference;

	public CollectionFetchableElementEntityGraph(
			CollectionReference collectionReference,
			ExpandingEntityQuerySpace entityQuerySpace) {
		super(
				entityQuerySpace,
				collectionReference.getPropertyPath().append( "<elements>" )
		);

		this.collectionReference = collectionReference;
	}

	@Override
	public CollectionReference getCollectionReference() {
		return collectionReference;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
