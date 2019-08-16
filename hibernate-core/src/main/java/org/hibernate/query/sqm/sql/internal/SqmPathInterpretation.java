/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.internal.BasicSqmPathSource;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.sql.ConversionException;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * @author Steve Ebersole
 */
public interface SqmPathInterpretation<T> extends SqmExpressionInterpretation<T> {
	NavigablePath getNavigablePath();

	@Override
	default SqmPathSource<T> getExpressableType() {
		return getSqmPathSource();
	}

	SqmPathSource<T> getSqmPathSource();

	@Override
	default Expression toSqlExpression(SqlAstCreationState sqlAstCreationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
//		final TableGroup tableGroup;
//
//		if ( getSqmPathSource() instanceof BasicSqmPathSource ) {
//			// maybe we should register the LHS TableGroup for the basic value
//			// under its NavigablePath, similar to what we do for embeddables
//			tableGroup = sqlAstCreationState.getFromClauseAccess().findTableGroup( getNavigablePath().getParent() );
//		}
//		else {
//			// for embeddable-, entity- and plural-valued Navigables we maybe do not have a TableGroup
//			final TableGroup thisTableGroup = sqlAstCreationState.getFromClauseAccess().findTableGroup( getNavigablePath() );
//			if ( thisTableGroup != null ) {
//				tableGroup = thisTableGroup;
//			}
//			else {
//				final NavigablePath lhsNavigablePath = getNavigablePath().getParent();
//				if ( lhsNavigablePath == null ) {
//					throw new ConversionException( "Could not find TableGroup to use - " + getNavigablePath().getFullPath() );
//				}
//				tableGroup = sqlAstCreationState.getFromClauseAccess().findTableGroup( lhsNavigablePath );
//			}
//		}
//
//		sqlAstCreationState.getCreationContext().getDomainModel().resolveMappingExpressable(  )
//
//		final List list = getNavigable().resolveColumnReferences( tableGroup, sqlAstCreationState );
//		if ( list.size() == 1 ) {
//			assert list.get( 0 ) instanceof Expression;
//			return (Expression) list.get( 0 );
//		}
//
//		return new SqlTuple( list, sqlAstCreationState.getExpressableType() );
	}
}
