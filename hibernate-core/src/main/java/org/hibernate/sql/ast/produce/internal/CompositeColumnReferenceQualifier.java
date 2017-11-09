/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.internal;

import java.util.Arrays;
import java.util.List;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.QualifiableSqlExpressable;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.from.TableReference;

/**
 * @author Steve Ebersole
 */
public class CompositeColumnReferenceQualifier implements ColumnReferenceQualifier {
	private final String uid;
	private final List<ColumnReferenceQualifier> concreteQualifiers;

	public CompositeColumnReferenceQualifier(
			String uid,
			List<ColumnReferenceQualifier> concreteQualifiers) {
		this.uid = uid;
		this.concreteQualifiers = concreteQualifiers;
	}

	public CompositeColumnReferenceQualifier(
			String uid,
			ColumnReferenceQualifier... concreteQualifiers) {
		this( uid, Arrays.asList( concreteQualifiers ) );
	}

	@Override
	public String getUniqueIdentifier() {
		return uid;
	}

	@Override
	public TableReference locateTableReference(Table table) {
		for ( ColumnReferenceQualifier concreteQualifier : concreteQualifiers ) {
			final TableReference tableReference = concreteQualifier.locateTableReference( table );
			if ( tableReference != null ) {
				return tableReference;
			}
		}

		return null;
	}

	@Override
	public ColumnReference resolveColumnReference(Column column) {
		for ( ColumnReferenceQualifier concreteQualifier : concreteQualifiers ) {
			final ColumnReference columnReference = concreteQualifier.resolveColumnReference( column );
			if ( columnReference != null ) {
				return columnReference;
			}
		}

		return null;
	}

	@Override
	public Expression qualify(QualifiableSqlExpressable sqlSelectable) {
		for ( ColumnReferenceQualifier concreteQualifier : concreteQualifiers ) {
			try {
				final Expression qualified = concreteQualifier.qualify( sqlSelectable );
				if ( qualified != null ) {
					return qualified;
				}
			}
			catch (Exception ignore) {
			}
		}

		return null;
	}
}
