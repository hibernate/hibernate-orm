/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.spi;

import org.hibernate.EntityMode;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.mapping.RootClass;
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
	<J> EntityPersister<J> getRootEntityPersister();

	InheritanceStrategy getInheritanceStrategy();

	EntityMode getEntityMode();

	<O,J> IdentifierDescriptor<O,J> getIdentifierDescriptor();
	<O,J> DiscriminatorDescriptor<O,J>  getDiscriminatorDescriptor();
	<O,J> VersionDescriptor<O,J>  getVersionDescriptor();
	<O,J> RowIdDescriptor<O,J>  getRowIdDescriptor();
	OptimisticLockStyle getOptimisticLockStyle();
	TenantDiscrimination getTenantDiscrimination();

	EntityRegionAccessStrategy getEntityRegionAccessStrategy();
	NaturalIdRegionAccessStrategy getNaturalIdRegionAccessStrategy();

	String getWhere();

	boolean isMutable();
	boolean isImplicitPolymorphismEnabled();

	void finishInitialization(PersisterCreationContext creationContext, RootClass mappingType);
}
