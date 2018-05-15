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
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;

/**
 * @author Steve Ebersole
 */
public class ToOneJoinCollectorImpl extends AbstractTableReferenceCollector {
	private SingularPersistentAttributeEntity attribute;
	private final TableSpace tableSpace;
	private final NavigableContainerReference lhs;
	private final NavigablePath navigablePath;
	private final String identificationVariable;
	private final LockMode lockMode;

	private Predicate predicate;

	@SuppressWarnings("WeakerAccess")
	public ToOneJoinCollectorImpl(
			SingularPersistentAttributeEntity attribute,
			TableSpace tableSpace,
			NavigableContainerReference lhs,
			NavigablePath navigablePath,
			String identificationVariable,
			LockMode lockMode) {
		this.attribute = attribute;
		this.tableSpace = tableSpace;
		this.lhs = lhs;
		this.navigablePath = navigablePath;
		this.identificationVariable = identificationVariable;
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
			addSecondaryReference( makeJoin( lhs.getColumnReferenceQualifier(), primaryTableReference ) );
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
					new RelationalPredicate(
							RelationalPredicate.Operator.EQUAL,
							referringColumnReference,
							targetColumnReference
					)
			);
		}

		return conjunction;
	}

	@SuppressWarnings("WeakerAccess")
	public TableGroupJoin generateTableGroup(JoinType joinType, String uid) {
		final EntityTableGroup joinedTableGroup = new EntityTableGroup(
				uid,
				tableSpace,
				lhs,
				attribute,
				lockMode,
				navigablePath,
				getPrimaryTableReference(),
				getTableReferenceJoins(),
				lhs == null ? null : lhs.getColumnReferenceQualifier()
		);

		Predicate predicate = null;

		if ( lhs != null ) {
			predicate = makePredicate( lhs.getColumnReferenceQualifier(), getPrimaryTableReference() );
		}


		return new TableGroupJoin( joinType, joinedTableGroup, predicate );
	}
}
