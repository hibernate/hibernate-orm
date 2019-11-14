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
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractEntityFecth implements Fetch {
	private final FetchParent fetchParent;
	private final SingularAssociationAttributeMapping fetchedAttribute;
	private final NavigablePath navigablePath;
	private final boolean nullable;
	private final LockMode lockMode;

	public AbstractEntityFecth(
			FetchParent fetchParent,
			SingularAssociationAttributeMapping fetchedAttribute,
			NavigablePath navigablePath,
			boolean nullable,
			LockMode lockMode) {
		this.fetchParent = fetchParent;
		this.fetchedAttribute = fetchedAttribute;
		this.navigablePath = navigablePath;
		this.nullable = nullable;
		this.lockMode = lockMode;
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
			FetchParentAccess parentAccess, Consumer<Initializer> collector, AssemblerCreationState creationState) {
		EntityInitializer entityInitializer = getEntityInitializer(
				parentAccess,
				collector,
				creationState
		);
		collector.accept( entityInitializer );
		return new EntityAssembler( getFetchedMapping().getJavaTypeDescriptor(), entityInitializer );
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	protected abstract EntityInitializer getEntityInitializer(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState);
}
