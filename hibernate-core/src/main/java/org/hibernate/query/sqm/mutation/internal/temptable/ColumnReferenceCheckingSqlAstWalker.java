/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * Visitor to determine if all visited column references use the same qualifier.
 */
public class ColumnReferenceCheckingSqlAstWalker extends AbstractSqlAstWalker {

	private final String identificationVariable;
	private boolean allColumnReferencesFromIdentificationVariable = true;

	public ColumnReferenceCheckingSqlAstWalker(String identificationVariable) {
		this.identificationVariable = identificationVariable;
	}

	public boolean isAllColumnReferencesFromIdentificationVariable() {
		return allColumnReferencesFromIdentificationVariable;
	}

	@Override
	public void visitSelectStatement(SelectStatement statement) {
		// Ignore subquery
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		if ( allColumnReferencesFromIdentificationVariable && !identificationVariable.equals( columnReference.getQualifier() ) ) {
			allColumnReferencesFromIdentificationVariable = false;
		}
	}

	@Override
	public void visitFilterPredicate(FilterPredicate filterPredicate) {
		allColumnReferencesFromIdentificationVariable = false;
	}

	@Override
	public void visitFilterFragmentPredicate(FilterPredicate.FilterFragmentPredicate fragmentPredicate) {
		allColumnReferencesFromIdentificationVariable = false;
	}

	@Override
	public void visitSqlFragmentPredicate(SqlFragmentPredicate predicate) {
		allColumnReferencesFromIdentificationVariable = false;
	}
}
