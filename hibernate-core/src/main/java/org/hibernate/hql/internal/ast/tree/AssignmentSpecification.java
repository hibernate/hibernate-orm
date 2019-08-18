/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;

import antlr.collections.AST;

/**
 * Encapsulates the information relating to an individual assignment within the
 * set clause of an HQL update statement.  This information is used during execution
 * of the update statements when the updates occur against "multi-table" stuff.
 *
 * @author Steve Ebersole
 */
public class AssignmentSpecification {
	private final Set tableNames;
	private final ParameterSpecification[] hqlParameters;
	private final AST eq;
	private final SessionFactoryImplementor factory;

	private String sqlAssignmentString;

	public AssignmentSpecification(AST eq, Queryable persister) {
		if ( eq.getType() != HqlSqlTokenTypes.EQ ) {
			throw new QueryException( "assignment in set-clause not associated with equals" );
		}

		this.eq = eq;
		this.factory = persister.getFactory();

		// Needed to bump this up to DotNode, because that is the only thing which currently
		// knows about the property-ref path in the correct format; it is either this, or
		// recurse over the DotNodes constructing the property path just like DotNode does
		// internally
		final DotNode lhs = (DotNode) eq.getFirstChild();
		final SqlNode rhs = (SqlNode) lhs.getNextSibling();

		validateLhs( lhs );

		final String propertyPath = lhs.getPropertyPath();
		Set<String> temp = new HashSet<>();
		// yuck!
		if ( persister instanceof UnionSubclassEntityPersister ) {
			final String[] tables = persister.getConstraintOrderedTableNameClosure();
			Collections.addAll( temp, tables );
		}
		else {
			temp.add(
					persister.getSubclassTableName( persister.getSubclassPropertyTableNumber( propertyPath ) )
			);
		}
		this.tableNames = Collections.unmodifiableSet( temp );

		if ( rhs == null ) {
			hqlParameters = new ParameterSpecification[0];
		}
		else if ( isParam( rhs ) ) {
			hqlParameters = new ParameterSpecification[] {( (ParameterNode) rhs ).getHqlParameterSpecification()};
		}
		else {
			List parameterList = ASTUtil.collectChildren(
					rhs,
					new ASTUtil.IncludePredicate() {
						public boolean include(AST node) {
							return isParam( node );
						}
					}
			);
			hqlParameters = new ParameterSpecification[parameterList.size()];
			Iterator itr = parameterList.iterator();
			int i = 0;
			while ( itr.hasNext() ) {
				hqlParameters[i++] = ( (ParameterNode) itr.next() ).getHqlParameterSpecification();
			}
		}
	}

	public boolean affectsTable(String tableName) {
		return this.tableNames.contains( tableName );
	}

	public ParameterSpecification[] getParameters() {
		return hqlParameters;
	}

	public String getSqlAssignmentFragment() {
		if ( sqlAssignmentString == null ) {
			try {
				SqlGenerator sqlGenerator = new SqlGenerator( factory );
				sqlGenerator.comparisonExpr(
						eq,
						false
				);  // false indicates to not generate parens around the assignment
				sqlAssignmentString = sqlGenerator.getSQL();
			}
			catch (Throwable t) {
				throw new QueryException( "cannot interpret set-clause assignment" );
			}
		}
		return sqlAssignmentString;
	}

	private static boolean isParam(AST node) {
		return node.getType() == HqlSqlTokenTypes.PARAM || node.getType() == HqlSqlTokenTypes.NAMED_PARAM;
	}

	private void validateLhs(FromReferenceNode lhs) {
		// make sure the lhs is "assignable"...
		if ( !lhs.isResolved() ) {
			throw new UnsupportedOperationException( "cannot validate assignablity of unresolved node" );
		}

		if ( lhs.getDataType().isCollectionType() ) {
			throw new QueryException( "collections not assignable in update statements" );
		}
		else if ( lhs.getDataType().isComponentType() ) {
			throw new QueryException( "Components currently not assignable in update statements" );
		}
		else if ( lhs.getDataType().isEntityType() ) {
			// currently allowed...
		}

		// TODO : why aren't these the same?
		if ( lhs.getImpliedJoin() != null || lhs.getFromElement().isImplied() ) {
			throw new QueryException( "Implied join paths are not assignable in update statements" );
		}
	}
}
