/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.loader.plan.spi.CollectionFetchableIndex;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;

/**
 * @author Gail Badner
 */
public class CollectionFetchableIndexAnyGraph extends AbstractAnyReference implements CollectionFetchableIndex {
	private final CollectionReference collectionReference;

	public CollectionFetchableIndexAnyGraph(CollectionReference collectionReference) {
		super(
				// this property path is just informational...
				collectionReference.getPropertyPath().append( "<index>" )
		);
		this.collectionReference = collectionReference;
	}

	@Override
	public CollectionReference getCollectionReference() {
		return collectionReference;
	}

	@Override
	public EntityReference resolveEntityReference() {
		return Fetch.class.isInstance( collectionReference ) ?
				Fetch.class.cast( collectionReference ).getSource().resolveEntityReference() :
				null;
	}
}
