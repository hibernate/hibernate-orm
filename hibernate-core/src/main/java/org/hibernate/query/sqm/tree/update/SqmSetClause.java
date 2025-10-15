/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import static java.util.Collections.unmodifiableList;

/**
 * @author Steve Ebersole
 */
public class SqmSetClause implements SqmCacheable {
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
		return unmodifiableList( assignments );
	}

	public void clearAssignments() {
		assignments.clear();
	}

	public void addAssignment(SqmAssignment<?> assignment) {
		assignments.add( assignment );
	}

	public <Y> void addAssignment(SqmPath<Y> targetPath, SqmExpression<? extends Y> value) {
		addAssignment( new SqmAssignment<>( targetPath, value ) );
	}

	public void appendHqlString(StringBuilder sb, SqmRenderContext context) {
		sb.append( " set " );
		appendAssignment( assignments.get( 0 ), sb, context );
		for ( int i = 1; i < assignments.size(); i++ ) {
			sb.append( ", " );
			appendAssignment( assignments.get( i ), sb, context );
		}
	}

	private static void appendAssignment(SqmAssignment<?> sqmAssignment, StringBuilder sb, SqmRenderContext context) {
		sqmAssignment.getTargetPath().appendHqlString( sb, context );
		sb.append( " = " );
		sqmAssignment.getValue().appendHqlString( sb, context );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmSetClause that
			&& Objects.equals( assignments, that.assignments );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( assignments );
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmSetClause that
			&& SqmCacheable.areCompatible( assignments, that.assignments );
	}

	@Override
	public int cacheHashCode() {
		return SqmCacheable.cacheHashCode( assignments );
	}
}
