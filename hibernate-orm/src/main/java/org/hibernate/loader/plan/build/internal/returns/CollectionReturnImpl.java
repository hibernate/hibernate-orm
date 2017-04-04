/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.persister.walking.spi.CollectionDefinition;

/**
 * @author Steve Ebersole
 */
public class CollectionReturnImpl extends AbstractCollectionReference implements CollectionReturn {

	public CollectionReturnImpl(CollectionDefinition collectionDefinition, ExpandingQuerySpaces querySpaces) {
		super(
				querySpaces.makeRootCollectionQuerySpace(
						querySpaces.generateImplicitUid(),
						collectionDefinition.getCollectionPersister()
				),
				new PropertyPath( "[" + collectionDefinition.getCollectionPersister().getRole() + "]" ),
				true
		);
	}
}
