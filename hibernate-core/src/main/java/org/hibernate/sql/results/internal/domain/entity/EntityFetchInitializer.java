/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.query.NavigablePath;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.EntityMappingNode;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * Initializer instance created from {@link EntityFetchImpl}
 *
 * @author Steve Ebersole
 */
public class EntityFetchInitializer extends AbstractEntityInitializer {
	private final FetchParentAccess parentAccess;

	public EntityFetchInitializer(
			FetchParentAccess parentAccess,
			EntityMappingNode resultDescriptor,
			NavigablePath navigablePath,
			LockMode lockMode,
			DomainResult identifierResult,
			DomainResult discriminatorResult,
			DomainResult versionResult,
			Consumer<Initializer> collector,
			AssemblerCreationContext context,
			AssemblerCreationState creationState) {
		super(
				resultDescriptor,
				navigablePath,
				lockMode,
				identifierResult,
				discriminatorResult,
				versionResult,
				collector,
				context,
				creationState
		);
		this.parentAccess = parentAccess;
	}

	@Override
	protected boolean isEntityReturn() {
		return false;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		// Use `parentAccess` to inject the parent instance into
		// the fetched entity
		super.finishUpRow( rowProcessingState );
	}

	@Override
	public String toString() {
		return "EntityFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
