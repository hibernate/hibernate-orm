/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression.domain;

import java.util.List;

import org.hibernate.internal.util.Loggable;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.sqm.spi.SqmExpressionInterpretation;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;

/**
 * Models a {@link Navigable} as an "intermediate resolution" of a
 * SQM node as we build the SQL AST
 *
 * @see org.hibernate.query.sqm.tree.select.SqmSelectableNode#accept
 *
 * @author Steve Ebersole
 */
public interface NavigableReference extends SqmExpressionInterpretation, Loggable {
	NavigablePath getNavigablePath();

	/**
	 * Get the Navigable referenced by this expression
	 *
	 * @return The Navigable
	 */
	Navigable<?> getNavigable();

	@Override
	default ExpressableType getExpressableType() {
		return getNavigable();
	}

	@Override
	default String toLoggableFragment() {
		return getNavigablePath().getFullPath();
	}

	@Override
	default DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return getNavigable().createDomainResult(
				getNavigablePath(),
				resultVariable,
				creationState
		);
	}

	@Override
	default Expression toSqlExpression(SqlAstCreationState sqlAstCreationState) {
		final TableGroup tableGroup;

		if ( getNavigable() instanceof BasicValuedNavigable ) {
			// maybe we should register the LHS TableGroup for the basic value
			// under its NavigablePath, similar to what we do for embeddables
			tableGroup = sqlAstCreationState.getFromClauseAccess().findTableGroup( getNavigablePath().getParent() );
		}
		else {
			// for embeddable-, entity- and plural-valued Navigables we maybe do not have a TableGroup
			final TableGroup thisTableGroup = sqlAstCreationState.getFromClauseAccess().findTableGroup( getNavigablePath() );
			if ( thisTableGroup != null ) {
				tableGroup = thisTableGroup;
			}
			else {
				final NavigablePath lhsNavigablePath = getNavigablePath().getParent();
				if ( lhsNavigablePath == null ) {
					throw new ParsingException( "Could not find TableGroup to use - " + getNavigablePath().getFullPath() );
				}
				tableGroup = sqlAstCreationState.getFromClauseAccess().findTableGroup( lhsNavigablePath );
			}
		}

		final List list = getNavigable().resolveColumnReferences( tableGroup, sqlAstCreationState );
		if ( list.size() == 1 ) {
			assert list.get( 0 ) instanceof Expression;
			return (Expression) list.get( 0 );
		}

		return new SqlTuple( list, getExpressableType() );
	}
}
