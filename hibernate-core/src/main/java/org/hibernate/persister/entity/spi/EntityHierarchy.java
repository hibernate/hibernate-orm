/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.spi;

import org.hibernate.mapping.RootClass;
import org.hibernate.persister.common.internal.SingularAttributeBasic;
import org.hibernate.persister.spi.PersisterCreationContext;

/**
 * Defines access to information across the entire entity hierarchy
 *
 * @author Steve Ebersole
 */
public interface EntityHierarchy {
	/**
	 * Access to the root entity for this hierarchy.
	 *
	 * @return The root entity for this hierarchy.
	 */
	EntityPersister getRootEntityPersister();

	InheritanceStrategy getInheritanceStrategy();

	IdentifierDescriptor getIdentifierDescriptor();
	SingularAttributeBasic getVersionAttribute();

	void finishInitialization(PersisterCreationContext creationContext, RootClass mappingType);
}
