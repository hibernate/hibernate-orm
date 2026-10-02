package org.hibernate.sql.ast.internal.model.builder;

import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.internal.TenantIdHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.model.builder.ColumnValueBindingBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableMutationBuilder;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;

import java.util.ArrayList;
import java.util.List;

/// Simplified builder for UPDATE statements for updating an entity's version.
///
/// @author Steve Ebersole
public class VersionUpdateBuilder implements TableMutationBuilder<TableUpdateStandard> {
	private final EntityPersister mutationTarget;
	private final MutatingTableReference tableReference;

	private final List<ColumnValueBinding> restrictionBindings = new ArrayList<>();
	private final List<ColumnValueBinding> tenantBindings = new ArrayList<>();
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
		final var tenantColumn = TenantIdHelper.tenantIdColumn( mutationTarget, tableReference.getTableName() );
		if ( tenantColumn != null ) {
			tenantBindings.add( ColumnValueBindingBuilder.createTenantRestriction(
					tenantColumn, tableReference,
					parameter -> parameterBinders.add( (ColumnValueParameter) parameter ) ) );
		}
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
		return new TableUpdateStandard(
				tableReference,
				mutationTarget,
				"update version for " + mutationTarget.getEntityName(),
				List.of( newVersionBinding ),
				restrictionBindings,
				tenantBindings,
				parameterBinders
		);
	}
}
