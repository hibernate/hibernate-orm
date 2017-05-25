/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.internal;

import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelDescriptorClassResolver;
import org.hibernate.metamodel.model.domain.NavigableResolutionException;
import org.hibernate.metamodel.model.domain.internal.SingleTableEntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardRuntimeModelDescriptorClassResolver implements RuntimeModelDescriptorClassResolver {
	@Override
	public Class<? extends EntityDescriptor> getEntityDescriptorClass(EntityMapping bootMapping) {
		if ( RootClass.class.isInstance( bootMapping ) ) {
			if ( bootMapping.getSubTypeMappings().isEmpty() ) {
				return singleTableEntityDescriptor();
			}
			else {
				//If the class has children, we need to find of which kind
				bootMapping = (PersistentClass) bootMapping.getSubTypeMappings().iterator().next();
			}
		}
		if ( JoinedSubclass.class.isInstance( bootMapping ) ) {
			return joinedSubclassEntityDescriptor();
		}
		else if ( UnionSubclass.class.isInstance( bootMapping ) ) {
			return unionSubclassEntityDescriptor();
		}
		else if ( SingleTableSubclass.class.isInstance( bootMapping ) ) {
			return singleTableEntityDescriptor();
		}
		else {
			throw new NavigableResolutionException(
					"Could not determine persister implementation for entity [" + bootMapping.getEntityName() + "]"
			);
		}
	}

	public Class<? extends EntityDescriptor> singleTableEntityDescriptor() {
		return SingleTableEntityDescriptor.class;
	}

	public Class<? extends EntityDescriptor> joinedSubclassEntityDescriptor() {
		throw new NotYetImplementedException(  );
//		return JoinedSubclassEntityPersister.class;
	}

	public Class<? extends EntityDescriptor> unionSubclassEntityDescriptor() {
		throw new NotYetImplementedException(  );
//		return UnionSubclassEntityPersister.class;
	}

	@Override
	public Class<? extends PersistentCollectionDescriptor> getCollectionDescriptorClass(Collection bootMapping) {
		return bootMapping.isOneToMany() ? oneToManyDescriptor() : basicCollectionDescriptor();
	}

	private Class<? extends PersistentCollectionDescriptor> oneToManyDescriptor() {
		throw new NotYetImplementedException(  );
	}

	private Class<? extends PersistentCollectionDescriptor> basicCollectionDescriptor() {
		throw new NotYetImplementedException(  );
	}
}
