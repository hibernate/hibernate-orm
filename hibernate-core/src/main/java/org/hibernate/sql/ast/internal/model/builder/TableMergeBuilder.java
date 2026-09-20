package org.hibernate.sql.ast.internal.model.builder;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.ast.spi.model.builder.AbstractTableUpdateBuilder;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.LogicalTableUpdate;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;
import org.hibernate.sql.model.internal.TableUpdateNoSet;
import static java.util.Collections.emptyList;

import java.util.List;

/**
 * @author Gavin King
 */
public class TableMergeBuilder<O extends MutationOperation> extends AbstractTableUpdateBuilder<O> {

	public TableMergeBuilder(
			EntityMutationTarget mutationTarget,
			TableMapping tableMapping,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableMapping, sessionFactory );
	}

	@Override
	protected EntityMutationTarget getMutationTarget() {
		return (EntityMutationTarget) super.getMutationTarget();
	}

	@SuppressWarnings("unchecked")
	@Override
	public LogicalTableUpdate<O> buildMutation() {
		final List<ColumnValueBinding> valueBindings = combine( getValueBindings(), getKeyBindings(), getLobValueBindings() );

		// TODO: add getMergeDetails()
//		if ( getMutatingTable().getTableMapping().getUpdateDetails().getCustomSql() != null ) {
//			return (RestrictedTableMutation<O>) new TableUpdateCustomSql(
//					getMutatingTable(),
//					getMutationTarget(),
//					getSqlComment(),
//					valueBindings,
//					getKeyRestrictionBindings(),
//					getOptimisticLockBindings()
//			);
//		}

		if ( isRowKnownToExist() ) {
			if ( valueBindings.isEmpty() ) {
				return (LogicalTableUpdate<O>) new TableUpdateNoSet( getMutatingTable(), getMutationTarget() );
			}
			return (LogicalTableUpdate<O>) new TableUpdateStandard(
					getMutatingTable(), getMutationTarget(), getSqlComment(), valueBindings,
					getKeyRestrictionBindings(), getOptimisticLockBindings(), null,
					getMutatingTable().getTableMapping().getUpdateDetails().getExpectation(), emptyList() );
		}

		return (LogicalTableUpdate<O>) new OptionalTableUpdate(
				getMutatingTable(),
				getMutationTarget(),
				getMutationTarget().getTargetPart().getVersionMapping() != null,
				valueBindings,
				getKeyRestrictionBindings(),
				getOptimisticLockBindings()
		);
	}
}
