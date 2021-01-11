/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.LockMode;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.entity.AbstractEntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;

/**
 * Initializer for cases where the entity is a root domain selection
 *
 * @author Steve Ebersole
 */
public class EntityResultInitializer extends AbstractEntityInitializer {
	private static final String CONCRETE_NAME = EntityResultInitializer.class.getSimpleName();

	public EntityResultInitializer(
			EntityResultGraphNode resultDescriptor,
			NavigablePath navigablePath,
			LockMode lockMode,
			DomainResult identifierResult,
			DomainResult discriminatorResult,
			DomainResult versionResult,
			DomainResult<Object> rowIdResult,
			AssemblerCreationState creationState) {
		super(
				resultDescriptor,
				navigablePath,
				lockMode,
				identifierResult,
				discriminatorResult,
				versionResult,
				rowIdResult,
				creationState
		);
	}

	@Override
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
	}

	@Override
	protected boolean isEntityReturn() {
		return true;
	}

	@Override
	public String toString() {
		return CONCRETE_NAME + "(" + getNavigablePath().getFullPath() + ")";
	}
}
