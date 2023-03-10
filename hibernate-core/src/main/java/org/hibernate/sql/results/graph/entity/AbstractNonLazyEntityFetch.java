/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.internal.EntityAssembler;

/**
 * Support for non-lazy EntityFetch implementations - both joined and subsequent-select
 *
 * @author Andrea Boriero
 */
public abstract class AbstractNonLazyEntityFetch extends AbstractFetchParent implements EntityFetch {
	private final FetchParent fetchParent;
	private final EntityValuedFetchable referencedModelPart;

	public AbstractNonLazyEntityFetch(
			FetchParent fetchParent,
			EntityValuedFetchable fetchedPart,
			NavigablePath navigablePath) {
		super( fetchedPart.getEntityMappingType(), navigablePath );
		this.referencedModelPart = fetchedPart;
		this.fetchParent = fetchParent;
	}

	@Override
	public EntityValuedFetchable getEntityValuedModelPart() {
		return referencedModelPart;
	}

	@Override
	public EntityValuedFetchable getReferencedModePart() {
		return getEntityValuedModelPart();
	}

	@Override
	public EntityValuedFetchable getReferencedMappingType() {
		return getEntityValuedModelPart();
	}

	@Override
	public EntityMappingType getReferencedMappingContainer() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	public EntityValuedFetchable getFetchedMapping() {
		return getEntityValuedModelPart();
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public DomainResultAssembler<?> createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final EntityInitializer entityInitializer = getEntityInitializer( parentAccess, creationState );
		return buildEntityAssembler( entityInitializer );
	}

	protected EntityAssembler buildEntityAssembler(EntityInitializer entityInitializer) {
		return new EntityAssembler( getFetchedMapping().getJavaType(), entityInitializer );
	}

	protected abstract EntityInitializer getEntityInitializer(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState);
}
