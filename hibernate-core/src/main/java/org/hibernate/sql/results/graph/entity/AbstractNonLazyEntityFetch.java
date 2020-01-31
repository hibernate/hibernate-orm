/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.internal.EntityAssembler;

/**
 * Support for non-lazy EntityFetch implementations - both joined and subsequent-select
 *
 * @author Andrea Boriero
 */
public abstract class AbstractNonLazyEntityFetch extends AbstractFetchParent implements EntityFetch {
	private final FetchParent fetchParent;
	private final boolean nullable;
	private final EntityValuedModelPart referencedModelPart;

	public AbstractNonLazyEntityFetch(
			FetchParent fetchParent,
			EntityValuedModelPart fetchedPart,
			NavigablePath navigablePath,
			boolean nullable) {
		super( fetchedPart.getEntityMappingType(), navigablePath );
		this.referencedModelPart = fetchedPart;
		this.fetchParent = fetchParent;
		this.nullable = nullable;
	}

	@Override
	public EntityMappingType getReferencedMappingType() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	public EntityMappingType getReferencedMappingContainer() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Fetchable getFetchedMapping() {
		return (Fetchable) getEntityValuedModelPart();
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		final EntityInitializer entityInitializer = getEntityInitializer(
				parentAccess,
				collector,
				creationState
		);
		collector.accept( entityInitializer );
		return new EntityAssembler( getFetchedMapping().getJavaTypeDescriptor(), entityInitializer );
	}

	protected abstract EntityInitializer getEntityInitializer(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState);

	@Override
	public EntityValuedModelPart getEntityValuedModelPart() {
		return referencedModelPart;
	}
}
