/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.internal.SingularAssociationAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class DelayedEntityFetchImpl implements Fetch {

	private FetchParent fetchParent;
	private SingularAssociationAttributeMapping fetchedAttribute;
	private final LockMode lockMode;
	private final NavigablePath navigablePath;
	private final boolean nullable;
	private DomainResult fkResult;
	private final DomainResultCreationState creationState;

	public DelayedEntityFetchImpl(
			FetchParent fetchParent,
			SingularAssociationAttributeMapping fetchedAttribute,
			LockMode lockMode,
			boolean nullable,
			NavigablePath navigablePath,
			DomainResult fkResult,
			DomainResultCreationState creationState) {
		this.fetchParent = fetchParent;
		this.fetchedAttribute = fetchedAttribute;
		this.lockMode = lockMode;
		this.nullable = nullable;
		this.navigablePath = navigablePath;
		this.fkResult = fkResult;
		this.creationState = creationState;

	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Fetchable getFetchedMapping() {
		return fetchedAttribute;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
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
		EntityInitializer entityInitializer = new DelayedEntityFetchInitializer(
				parentAccess,
				navigablePath,
				fetchedAttribute.getMappedFetchStrategy(),
				lockMode,
				(EntityPersister) fetchedAttribute.getMappedTypeDescriptor(),
				fkResult.createResultAssembler( collector, creationState )
		);
		collector.accept( entityInitializer );
		return new EntityAssembler( fetchedAttribute.getJavaTypeDescriptor(), entityInitializer );
	}
}
