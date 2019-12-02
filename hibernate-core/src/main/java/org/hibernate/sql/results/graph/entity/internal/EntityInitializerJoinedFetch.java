/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.sql.results.graph.entity.AbstractEntityInitializer;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.graph.Initializer;

/**
 * @author Andrea Boriero
 */
public class EntityInitializerJoinedFetch extends AbstractEntityInitializer {
	protected EntityInitializerJoinedFetch(
			EntityResultGraphNode resultDescriptor,
			NavigablePath navigablePath,
			LockMode lockMode,
			DomainResult<?> identifierResult,
			DomainResult<?> discriminatorResult,
			DomainResult<?> versionResult,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationState) {
		super(
				resultDescriptor,
				navigablePath,
				lockMode,
				identifierResult,
				discriminatorResult,
				versionResult,
				initializerConsumer,
				creationState
		);
	}

	@Override
	protected boolean isEntityReturn() {
		return false;
	}

	@Override
	public String toString() {
		return "EntityFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
