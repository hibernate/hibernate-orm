/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.AbstractTableReferenceCollector;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class ToOneJoinCollectorImpl extends AbstractTableReferenceCollector {
	private final NavigablePath navigablePath;
	private final SingularPersistentAttributeEntity attribute;
	private final TableGroup lhs;
	private final String explicitSourceAlias;
	private final LockMode lockMode;

	private Predicate predicate;

	@SuppressWarnings("WeakerAccess")
	public ToOneJoinCollectorImpl(
			NavigablePath navigablePath,
			SingularPersistentAttributeEntity attribute,
			TableGroup lhs,
			String explicitSourceAlias,
			LockMode lockMode) {
		this.navigablePath = navigablePath;
		this.attribute = attribute;
		this.lhs = lhs;
		this.explicitSourceAlias = explicitSourceAlias;
		this.lockMode = lockMode;
	}

	/**
	 * @implNote Here, the incoming `primaryTableReference` is the root
	 * table reference for the associated entity.
	 */
	@Override
	public void addPrimaryReference(TableReference primaryTableReference) {
		if ( getPrimaryTableReference() == null ) {
			super.addPrimaryReference( primaryTableReference );
			return;
		}

		// if we have a lhs, try to add the primary ref as a secondary ref
		if ( lhs != null ) {
			addSecondaryReference( makeJoin( lhs, primaryTableReference ) );
		}
	}

	private TableReferenceJoin makeJoin(ColumnReferenceQualifier lhs, TableReference rootTableReference) {
		return new TableReferenceJoin(
				JoinType.LEFT,
				rootTableReference,
				makePredicate( lhs, rootTableReference )
		);
	}

	private Predicate makePredicate(ColumnReferenceQualifier lhs, TableReference rhs) {
		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );

		for ( ForeignKey.ColumnMappings.ColumnMapping columnMapping : attribute.getForeignKey().getColumnMappings().getColumnMappings() ) {
			final ColumnReference referringColumnReference = lhs.resolveColumnReference( columnMapping.getReferringColumn() );
			final ColumnReference targetColumnReference = rhs.resolveColumnReference( columnMapping.getTargetColumn() );

			// todo (6.0) : we need some kind of validation here that the column references are properly defined

			// todo (6.0) : we could also handle this using SQL row-value syntax, e.g.:
			//		`... where ... [ (rCol1, rCol2, ...) = (tCol1, tCol2, ...) ] ...`

			conjunction.add(
					new ComparisonPredicate(
							referringColumnReference, ComparisonOperator.EQUAL,
							targetColumnReference
					)
			);
		}

		return conjunction;
	}

	public TableGroupJoin generateTableGroup(JoinType joinType, String uid) {
		final StandardTableGroup joinedTableGroup = new StandardTableGroup(
				uid,
				navigablePath,
				attribute,
				lockMode,
				getPrimaryTableReference(),
				getTableReferenceJoins(),
				lhs
		);

		Predicate predicate = null;

		if ( lhs != null ) {
			predicate = makePredicate( lhs, getPrimaryTableReference() );
		}


		return new TableGroupJoin( joinType, joinedTableGroup, predicate );
	}
}
