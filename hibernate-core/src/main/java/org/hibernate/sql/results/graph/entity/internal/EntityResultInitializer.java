/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.LockMode;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.entity.AbstractEntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

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
			Fetch identifierFetch,
			BasicFetch<?> discriminatorFetch,
			DomainResult<Object> rowIdResult,
			AssemblerCreationState creationState) {
		super(
				resultDescriptor,
				navigablePath,
				lockMode,
				identifierFetch,
				discriminatorFetch,
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
		return CONCRETE_NAME + "(" + getNavigablePath() + ")";
	}

	@Override
	protected Object getEntityInstanceFromExecutionContext(RowProcessingState rowProcessingState) {
		final ExecutionContext executionContext = rowProcessingState.getJdbcValuesSourceProcessingState()
				.getExecutionContext();
		final Object entityInstanceFromExecutionContext = executionContext.getEntityInstance();
		if ( entityInstanceFromExecutionContext != null
				&& getEntityKey().getIdentifier().equals( executionContext.getEntityId() ) ) {
			return entityInstanceFromExecutionContext;
		}
		return null;
	}

}
