/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.update;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmSetClause {
	private final List<SqmAssignment<?>> assignments;

	public SqmSetClause() {
		this.assignments = new ArrayList<>();
	}

	private SqmSetClause(List<SqmAssignment<?>> assignments) {
		this.assignments = assignments;
	}

	public SqmSetClause copy(SqmCopyContext context) {
		final List<SqmAssignment<?>> assignments = new ArrayList<>( this.assignments.size() );
		for ( SqmAssignment<?> assignment : this.assignments ) {
			assignments.add( assignment.copy( context ) );
		}
		return new SqmSetClause( assignments );
	}

	public List<SqmAssignment<?>> getAssignments() {
		return Collections.unmodifiableList( assignments );
	}

	public void addAssignment(SqmAssignment<?> assignment) {
		assignments.add( assignment );
	}

	public <Y> void addAssignment(SqmPath<Y> targetPath, SqmExpression<? extends Y> value) {
		addAssignment( new SqmAssignment<>( targetPath, value ) );
	}

	public void appendHqlString(StringBuilder sb) {
		sb.append( " set " );
		appendAssignment( assignments.get( 0 ), sb );
		for ( int i = 1; i < assignments.size(); i++ ) {
			sb.append( ", " );
			appendAssignment( assignments.get( i ), sb );
		}
	}

	private static void appendAssignment(SqmAssignment<?> sqmAssignment, StringBuilder sb) {
		sqmAssignment.getTargetPath().appendHqlString( sb );
		sb.append( " = " );
		sqmAssignment.getValue().appendHqlString( sb );
	}
}
