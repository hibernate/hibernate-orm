/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.SingularAssociationAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityFetchDelayedImpl extends AbstractNonJoinedEntityFetch {
	private final LockMode lockMode;
	private final boolean nullable;

	private final DomainResult keyResult;

	public EntityFetchDelayedImpl(
			FetchParent fetchParent,
			SingularAssociationAttributeMapping fetchedAttribute,
			LockMode lockMode,
			boolean nullable,
			NavigablePath navigablePath,
			DomainResult keyResult) {
		super( navigablePath, fetchedAttribute, fetchParent );
		this.lockMode = lockMode;
		this.nullable = nullable;

		this.keyResult = keyResult;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.DELAYED;
	}

	@Override
	public boolean hasTableGroup() {
		return false;
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		final EntityInitializer entityInitializer = new EntityFetchDelayedInitializer(
				getNavigablePath(),
				getEntityValuedModelPart().getEntityMappingType().getEntityPersister(),
				keyResult.createResultAssembler( collector, creationState )
		);
		collector.accept( entityInitializer );
		return new EntityAssembler( getFetchedMapping().getJavaTypeDescriptor(), entityInitializer );
	}
}
