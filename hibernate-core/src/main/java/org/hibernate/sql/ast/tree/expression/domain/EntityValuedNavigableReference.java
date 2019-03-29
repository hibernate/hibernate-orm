/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression.domain;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.sqm.spi.SqmUpdateToSqlAstConverterMultiTable;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignment;

/**
 * @author Steve Ebersole
 */
public class EntityValuedNavigableReference implements NavigableContainerReference, AssignableNavigableReference {
	private final NavigablePath navigablePath;
	private final EntityValuedNavigable navigable;
	private final LockMode lockMode;

	public EntityValuedNavigableReference(
			NavigablePath navigablePath,
			EntityValuedNavigable navigable,
			LockMode lockMode,
			@SuppressWarnings("unused") SqlAstCreationState creationState) {
		this.navigablePath = navigablePath;
		this.navigable = navigable;
		this.lockMode = lockMode;
	}


	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public EntityValuedNavigable getNavigable() {
		return navigable;
	}

	@Override
	public String toString() {
		return '`' + getNavigablePath().getFullPath() + '`';
	}

	@Override
	public void applySqlAssignments(
			Expression newValueExpression,
			SqmUpdateToSqlAstConverterMultiTable.AssignmentContext assignmentProcessingState,
			Consumer<Assignment> assignmentConsumer,
			SqlAstCreationContext creationContext) {
		if ( getNavigable() instanceof SingularPersistentAttributeEntity ) {
			final EntityIdentifier identifierDescriptor = getNavigable()
					.getEntityDescriptor()
					.getIdentifierDescriptor();

			if ( identifierDescriptor instanceof EntityIdentifierSimple ) {
				final Column boundColumn = ( (EntityIdentifierSimple) identifierDescriptor ).getBoundColumn();
				final TableReference tableReference = assignmentProcessingState.resolveTableReference( boundColumn.getSourceTable() );
				assignmentConsumer.accept(
						new Assignment(
								tableReference.resolveColumnReference( boundColumn ),
								newValueExpression
						)
				);
			}
			else {
				// the trouble is breaking down the `newValueExpression` into its constituent SQL pieces
				throw new NotYetImplementedFor6Exception();
			}

		}
		else {
			throw new UnsupportedOperationException( "Navigable type is not singular attribute : " + getNavigable() );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// These were meant to help with re-usable implicit joins.

}
