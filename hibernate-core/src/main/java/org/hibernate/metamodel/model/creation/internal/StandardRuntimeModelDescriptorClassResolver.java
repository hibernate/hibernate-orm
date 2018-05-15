/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelDescriptorClassResolver;
import org.hibernate.metamodel.model.domain.NavigableResolutionException;
import org.hibernate.metamodel.model.domain.internal.entity.JoinedEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.internal.PersistentArrayDescriptorImpl;
import org.hibernate.metamodel.model.domain.internal.PersistentBagDescriptorImpl;
import org.hibernate.metamodel.model.domain.internal.PersistentListDescriptorImpl;
import org.hibernate.metamodel.model.domain.internal.PersistentMapDescriptorImpl;
import org.hibernate.metamodel.model.domain.internal.PersistentSetDescriptorImpl;
import org.hibernate.metamodel.model.domain.internal.entity.SingleTableEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardRuntimeModelDescriptorClassResolver implements RuntimeModelDescriptorClassResolver {
	@Override
	public Class<? extends EntityTypeDescriptor> getEntityDescriptorClass(EntityMapping bootMapping) {
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

	public Class<? extends EntityTypeDescriptor> singleTableEntityDescriptor() {
		return SingleTableEntityTypeDescriptor.class;
	}

	public Class<? extends EntityTypeDescriptor> joinedSubclassEntityDescriptor() {
		return JoinedEntityTypeDescriptor.class;
	}

	public Class<? extends EntityTypeDescriptor> unionSubclassEntityDescriptor() {
		throw new NotYetImplementedException(  );
//		return UnionSubclassEntityPersister.class;
	}

	@Override
	public Class<? extends PersistentCollectionDescriptor> getCollectionDescriptorClass(Collection bootMapping) {
		if ( bootMapping instanceof Bag ) {
			return PersistentBagDescriptorImpl.class;
		}

		if ( bootMapping instanceof Array ) {
			return PersistentArrayDescriptorImpl.class;
		}

		if ( bootMapping instanceof List ) {
			return PersistentListDescriptorImpl.class;
		}

		if ( bootMapping instanceof Set ) {
			return PersistentSetDescriptorImpl.class;
		}

		if ( bootMapping instanceof Map ) {
			return PersistentMapDescriptorImpl.class;
		}

		throw new HibernateException( "Unsure which PersistentCollectionDescriptor impl class to use - " + bootMapping );
	}
}
