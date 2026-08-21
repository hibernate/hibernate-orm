/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.update.UpdateStatement;

/**
 * A walker that detects both nested correlation and target table correlation in a single pass.
 *
 * @author Yoobin Yoon
 */
public class NestedOrTargetTableCorrelationVisitor extends NestedCorrelationChecker {

	private final String targetAlias;

	public static boolean hasCorrelation(MutationStatement statement) {
		final String targetAlias = statement.getTargetTable().getIdentificationVariable();
		if ( targetAlias == null ) {
			return false;
		}
		try {
			if ( statement instanceof DeleteStatement deleteStatement ) {
				if ( deleteStatement.getRestriction() != null ) {
					deleteStatement.getRestriction().accept( new NestedOrTargetTableCorrelationVisitor( targetAlias ) );
				}
			}
			else if ( statement instanceof UpdateStatement updateStatement ) {
				final NestedOrTargetTableCorrelationVisitor visitor = new NestedOrTargetTableCorrelationVisitor(
						targetAlias );
				if ( updateStatement.getAssignments() != null ) {
					for ( var assignment : updateStatement.getAssignments() ) {
						assignment.getAssignedValue().accept( visitor );
					}
				}
				if ( updateStatement.getRestriction() != null ) {
					updateStatement.getRestriction().accept( visitor );
				}
			}
			return false;
		}
		catch (NestedCorrelationException ex) {
			return true;
		}
	}

	private NestedOrTargetTableCorrelationVisitor(String targetAlias) {
		this.targetAlias = targetAlias;
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		String qualifier = columnReference.getQualifier();
		if ( currentLevelAliases != null && targetAlias.equals( qualifier ) ) {
			throw new NestedCorrelationException();
		}
		super.visitColumnReference( columnReference );
	}
}
