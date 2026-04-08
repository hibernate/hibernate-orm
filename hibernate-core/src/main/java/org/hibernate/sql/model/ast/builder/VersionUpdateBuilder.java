/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.action.queue.meta.TableDescriptorAsTableMapping;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.internal.TableUpdateStandard;

import java.util.ArrayList;
import java.util.List;

/// Simplified builder for UPDATE statements for updating an entity's version.
///
/// @author Steve Ebersole
public class VersionUpdateBuilder implements TableMutationBuilder<TableUpdateStandard> {
	private final EntityPersister mutationTarget;
	private final MutatingTableReference tableReference;

	private final List<ColumnValueBinding> restrictionBindings = new ArrayList<>();
	private final ColumnValueBinding newVersionBinding;

	private final List<ColumnValueParameter> parameterBinders;

	public VersionUpdateBuilder(EntityPersister mutationTarget) {
		this.mutationTarget = mutationTarget;

		var identifierTableDescriptor = mutationTarget.getIdentifierTableDescriptor();
		parameterBinders = CollectionHelper.arrayList(
				1 + 1 + identifierTableDescriptor.keyDescriptor().columns().size()
		);

		this.tableReference = new MutatingTableReference( new TableDescriptorAsTableMapping(
				identifierTableDescriptor,
				0,
				true,
				false
		) );

		newVersionBinding = ColumnValueBindingBuilder.createValueBinding(
				"?",
				mutationTarget.getVersionMapping(),
				tableReference,
				ParameterUsage.SET,
				(o) -> parameterBinders.add( (ColumnValueParameter) o )
		);

		identifierTableDescriptor.keyDescriptor().columns().forEach( (columnDescriptor) -> {
			var idColumnBinding = ColumnValueBindingBuilder.createValueBinding(
					"?",
					columnDescriptor,
					tableReference,
					ParameterUsage.RESTRICT,
					(o) -> parameterBinders.add( (ColumnValueParameter) o )
			);
			restrictionBindings.add( idColumnBinding );
		} );

		var oldVersionBinding = ColumnValueBindingBuilder.createValueBinding(
				"?",
				mutationTarget.getVersionMapping(),
				tableReference,
				ParameterUsage.RESTRICT,
				(o) -> parameterBinders.add( (ColumnValueParameter) o )
		);
		restrictionBindings.add( oldVersionBinding );
	}

	@Override
	public MutatingTableReference getMutatingTable() {
		return null;
	}

	@Override
	public boolean hasValueBindings() {
		return false;
	}

	@Override
	public TableUpdateStandard buildMutation() {
		var sqlBuffer = new StringBuilder( "update " );
		sqlBuffer.append( tableReference.getTableName() );
		sqlBuffer.append( " set " ).append( newVersionBinding.getColumnReference().getColumnExpression() ).append( " = ? " );
		sqlBuffer.append( " where " );
		boolean first = true;
		for ( int i = 0; i < restrictionBindings.size(); i++ ) {
			if ( !first ) {
				sqlBuffer.append( " and " );
			}
			first = false;

			var restrictionBinding = restrictionBindings.get( i );
			sqlBuffer.append( restrictionBinding.getColumnReference().getColumnExpression() ).append( " = ? " );
		}

		var sql = sqlBuffer.toString();
		return new TableUpdateStandard(
				tableReference,
				mutationTarget,
				sql,
				List.of( newVersionBinding ),
				restrictionBindings,
				List.of(),
				parameterBinders
		);
	}
}
