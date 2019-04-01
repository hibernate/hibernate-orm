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
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.SyntaxException;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.sqm.spi.SqmUpdateToSqlAstConverterMultiTable;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class EmbeddableValuedNavigableReference implements NavigableContainerReference, AssignableNavigableReference {
	private final NavigablePath navigablePath;
	private final EmbeddedValuedNavigable<?> navigable;
	private final TableGroup ownerTableGroup;
	private final LockMode lockMode;

	public EmbeddableValuedNavigableReference(
			NavigablePath navigablePath,
			EmbeddedValuedNavigable navigable,
			LockMode lockMode,
			SqlAstCreationState creationState) {
		this.navigablePath = navigablePath;
		this.navigable = navigable;
		this.lockMode = lockMode;

		// the TableGroup for any embeddable value is defined by its container/parent
		this.ownerTableGroup = creationState.getFromClauseAccess().findTableGroup( navigablePath.getParent() );

		// but we also want to make sure it is registered under our NavigablePath as well for any de-references from the embedded
		creationState.getFromClauseAccess().registerTableGroup( navigablePath, ownerTableGroup );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigableContainer<?> getNavigable() {
		return navigable;
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final TypeConfiguration typeConfiguration = creationState.getSqlAstCreationState()
				.getCreationContext()
				.getDomainModel()
				.getTypeConfiguration();

		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();

		final TableGroup tableGroup = creationState.getFromClauseAccess()
				.findTableGroup( getNavigablePath().getParent() );
		if ( tableGroup == null ) {
			throw new SyntaxException( "Could not locate TableGroup : " + getNavigablePath().getFullPath() );
		}

		getNavigable().visitColumns(
				(sqlExpressableType, column) -> sqlExpressionResolver.resolveSqlSelection(
						sqlExpressionResolver.resolveSqlExpression( tableGroup, column ),
						sqlExpressableType.getJavaTypeDescriptor(),
						typeConfiguration
				),
				Clause.SELECT,
				typeConfiguration
		);
	}

	@Override
	public void applySqlAssignments(
			Expression newValueExpression,
			SqmUpdateToSqlAstConverterMultiTable.AssignmentContext assignmentProcessingState,
			Consumer<Assignment> assignmentConsumer,
			SqlAstCreationContext creationContext) {
		// `newValueExpression` can be:
		//		1) parameter
		//		2) tuple
		//		3) EmbeddableValuedNavigableReference
		//		n) others?

		// todo (6.0) : rather than passing in `newValueExpression` directly it might be necessary to
		//		pass in some form of "supplier".  E.g., consider
		//
		//	... from Person p where p.name = ?
		//
		//		assuming `Person#name` is composite, that single SqmParameter would need to become
		//		2 JdbcParameters (one for first-name, one for last-name).  This is the effect of
		//		type inference at this level

		// the trouble is breaking down the `newValueExpression` into its constituent SQL pieces
		throw new NotYetImplementedFor6Exception();
//		navigable.getEmbeddedDescriptor().dehydrate(
//				navigable.getEmbeddedDescriptor().unresolve(
//
//				)
//		);
//		navigable.getEmbeddedDescriptor().visitColumns(
//				(sqlExpressableType, column) -> {
//					final TableReference tableReference = assignmentProcessingState.resolveTableReference( column.getSourceTable() );
//
//					assignmentConsumer.accept(
//							new Assignment(
//									tableReference.resolveColumnReference( column ),
//									newValueExpression
//							)
//					);
//				},
//				Clause.UPDATE,
//				creationContext.getDomainModel().getTypeConfiguration()
//		);
	}
}
