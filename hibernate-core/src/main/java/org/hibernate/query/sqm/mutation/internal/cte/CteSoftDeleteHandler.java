/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;

/**
 * Specialized CteDeleteHandler for soft-delete handling
 *
 * @author Steve Ebersole
 */
public class CteSoftDeleteHandler extends CteDeleteHandler {
	protected CteSoftDeleteHandler(
			CteTable cteTable,
			SqmDeleteStatement<?> sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			CteMutationStrategy strategy,
			SessionFactoryImplementor sessionFactory) {
		super( cteTable, sqmDeleteStatement, domainParameterXref, strategy, sessionFactory );
	}

	protected void applyDmlOperations(
			CteContainer statement,
			CteStatement idSelectCte,
			SessionFactoryImplementor factory,
			TableGroup updatingTableGroup) {
		final SoftDeleteMapping softDeleteMapping = getEntityDescriptor().getSoftDeleteMapping();
		final TableDetails softDeleteTable = getEntityDescriptor().getSoftDeleteTableDetails();
		final CteTable dmlResultCte = new CteTable(
				getCteTableName( softDeleteTable.getTableName() ),
				idSelectCte.getCteTable().getCteColumns()
		);
		final TableReference updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				softDeleteTable.getTableName(),
				true
		);
		final NamedTableReference dmlTableReference = resolveUnionTableReference(
				updatingTableReference,
				softDeleteTable.getTableName()
		);

		final List<ColumnReference> columnReferences = new ArrayList<>( idSelectCte.getCteTable().getCteColumns().size() );
		final TableDetails.KeyDetails keyDetails = softDeleteTable.getKeyDetails();
		keyDetails.forEachKeyColumn( (position, selectable) -> columnReferences.add(
				new ColumnReference( dmlTableReference, selectable )
		) );

		final ColumnReference softDeleteColumnReference = new ColumnReference(
				dmlTableReference,
				softDeleteMapping
		);
		final JdbcLiteral<?> deletedIndicator = new JdbcLiteral<>(
				softDeleteMapping.getDeletedLiteralValue(),
				softDeleteMapping.getJdbcMapping()
		);
		final Assignment assignment = new Assignment(
				softDeleteColumnReference,
				deletedIndicator
		);

		final MutationStatement dmlStatement = new UpdateStatement(
				dmlTableReference,
				Collections.singletonList( assignment ),
				createIdSubQueryPredicate( columnReferences, idSelectCte, factory ),
				columnReferences
		);
		statement.addCteStatement( new CteStatement( dmlResultCte, dmlStatement ) );
	}
}
