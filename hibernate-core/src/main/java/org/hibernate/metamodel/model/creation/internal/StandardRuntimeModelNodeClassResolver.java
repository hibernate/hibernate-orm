/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.internal;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelNodeClassResolver;
import org.hibernate.metamodel.model.domain.NavigableResolutionException;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;

/**
 * @author Steve Ebersole
 */
public class StandardRuntimeModelNodeClassResolver implements RuntimeModelNodeClassResolver {

	@Override
	public Class<? extends EntityDescriptor> getEntityPersisterClass(PersistentClass metadata) {
		// todo : make sure this is based on an attribute kept on the metamodel in the new code, not the concrete PersistentClass impl found!
		if ( RootClass.class.isInstance( metadata ) ) {
			if ( metadata.hasSubclasses() ) {
				//If the class has children, we need to find of which kind
				metadata = (PersistentClass) metadata.getDirectSubclasses().next();
			}
			else {
				return singleTableEntityPersister();
			}
		}
		if ( JoinedSubclass.class.isInstance( metadata ) ) {
			return joinedSubclassEntityPersister();
		}
		else if ( UnionSubclass.class.isInstance( metadata ) ) {
			return unionSubclassEntityPersister();
		}
		else if ( SingleTableSubclass.class.isInstance( metadata ) ) {
			return singleTableEntityPersister();
		}
		else {
			throw new NavigableResolutionException(
					"Could not determine persister implementation for entity [" + metadata.getEntityName() + "]"
			);
		}
	}

	public Class<? extends EntityDescriptor> singleTableEntityPersister() {
		return SingleTableEntityPersister.class;
	}

	public Class<? extends EntityDescriptor> joinedSubclassEntityPersister() {
		return JoinedSubclassEntityPersister.class;
	}

	public Class<? extends EntityDescriptor> unionSubclassEntityPersister() {
		return UnionSubclassEntityPersister.class;
	}

	@Override
	public Class<? extends PersistentCollectionDescriptor> getCollectionPersisterClass(Collection metadata) {
		return metadata.isOneToMany() ? oneToManyPersister() : basicCollectionPersister();
	}
}
