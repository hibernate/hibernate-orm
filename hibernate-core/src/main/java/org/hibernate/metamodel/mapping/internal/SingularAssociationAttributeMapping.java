/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public class SingularAssociationAttributeMapping extends AbstractSingularAttributeMapping implements EntityValuedModelPart {
	public SingularAssociationAttributeMapping(
			String name,
			int stateArrayPosition,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchStrategy mappedFetchStrategy,
			MappingType type,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super(
				name,
				stateArrayPosition,
				attributeMetadataAccess,
				mappedFetchStrategy,
				type,
				declaringType,
				propertyAccess
		);
	}

	@Override
	public EntityMappingType getEntityMappingType() {
		return null;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		return null;
	}
}
