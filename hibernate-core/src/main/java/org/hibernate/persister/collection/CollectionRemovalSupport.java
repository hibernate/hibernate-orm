package org.hibernate.persister.collection;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.RestrictedTableMutation;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;

/// Legacy collection removal SQL and the assignment values required by that SQL.
public interface CollectionRemovalSupport {
	CollectionRemovalSupport STANDARD = new CollectionRemovalSupport() {};

	CollectionRemovalSupport SOFT_DELETE = new CollectionRemovalSupport() {
		@Override
		public RestrictedTableMutation<JdbcMutationOperation> buildDeleteRow(
				BasicCollectionPersister persister,
				MutatingTableReference table) {
			return persister.generateSoftDeleteRowsAst( table );
		}

		@Override
		public RestrictedTableMutation<JdbcMutationOperation> buildRemoveAll(
				BasicCollectionPersister persister,
				MutatingTableReference table) {
			return CollectionRemovalUpdate.softDelete( persister, table )
					.buildRemoveAll( persister, table );
		}
	};

	CollectionRemovalSupport TEMPORAL = new CollectionRemovalSupport() {
		@Override
		public RestrictedTableMutation<JdbcMutationOperation> buildDeleteRow(
				BasicCollectionPersister persister,
				MutatingTableReference table) {
			return persister.generateTemporalDeleteRowsAst( table );
		}

		@Override
		public RestrictedTableMutation<JdbcMutationOperation> buildRemoveAll(
				BasicCollectionPersister persister,
				MutatingTableReference table) {
			return persister.generateTemporalDeleteAllAst( table );
		}

		@Override
		public void bindValues(
				PluralAttributeMapping attribute,
				String tableName,
				SharedSessionContractImplementor session,
				JdbcValueBindings bindings) {
			if ( TemporalMutationHelper.isUsingParameters( session ) ) {
				final var temporal = attribute.getTemporalMapping();
				assert temporal != null;
				bindings.bindValue( session.getCurrentChangesetIdentifier(), tableName,
						temporal.getEndingColumnMapping().getSelectionExpression(), ParameterUsage.SET );
			}
		}
	};

	default RestrictedTableMutation<JdbcMutationOperation> buildDeleteRow(
			BasicCollectionPersister persister,
			MutatingTableReference table) {
		return persister.generatePhysicalDeleteRowAst( table );
	}

	default RestrictedTableMutation<JdbcMutationOperation> buildRemoveAll(
			BasicCollectionPersister persister,
			MutatingTableReference table) {
		return persister.generatePhysicalDeleteAllAst( table );
	}

	default void bindValues(
			PluralAttributeMapping attribute,
			String tableName,
			SharedSessionContractImplementor session,
			JdbcValueBindings bindings) {
	}
}
