/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
			SessionFactoryImplementor sessionFactory,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		super( cteTable, sqmDeleteStatement, domainParameterXref, strategy, sessionFactory, context, firstJdbcParameterBindingsConsumer );
	}

	@Override
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

		final Assignment assignment = softDeleteMapping.createSoftDeleteAssignment( dmlTableReference );
		final MutationStatement dmlStatement = new UpdateStatement(
				dmlTableReference,
				Collections.singletonList( assignment ),
				createIdSubQueryPredicate( columnReferences, idSelectCte, factory ),
				columnReferences
		);
		statement.addCteStatement( new CteStatement( dmlResultCte, dmlStatement ) );
	}
}
