package org.hibernate.persister.collection;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameterList;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.RestrictedTableMutation;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;

/// The assignment that removes a row and the restriction that identifies a live row.
record CollectionRemovalUpdate(
		ColumnValueBinding assignment,
		ColumnValueBinding restriction,
		String comment,
		@Nullable Expectation removeAllExpectation) {
	static CollectionRemovalUpdate softDelete(BasicCollectionPersister persister, MutatingTableReference table) {
		final var mapping = persister.getAttributeMapping().getSoftDeleteMapping();
		assert mapping != null;
		final var indicator = new ColumnReference( table, mapping );
		return new CollectionRemovalUpdate(
				mapping.createDeletedValueBinding( indicator ),
				mapping.createNonDeletedValueBinding( indicator ),
				"soft-delete removal",
				null );
	}

	static CollectionRemovalUpdate temporal(BasicCollectionPersister persister, MutatingTableReference table) {
		final var mapping = persister.getAttributeMapping().getTemporalMapping();
		assert mapping != null;
		final var end = new ColumnReference( table, mapping.getEndingColumnMapping() );
		return new CollectionRemovalUpdate(
				mapping.createEndingValueBinding( end ),
				mapping.createNullEndingValueBinding( end ),
				"temporal removal",
				// Closing a collection may affect any number of rows.
				table.getTableMapping().getDeleteDetails().getExpectation() );
	}

	RestrictedTableMutation<JdbcMutationOperation> buildRow(BasicCollectionPersister persister, MutatingTableReference table) {
		final var builder = new TableUpdateBuilderStandard<JdbcMutationOperation>(
				persister, table, persister.getFactory(), persister.getSqlWhereString() );
		persister.applyRowDeleteRestrictions( builder );
		builder.addValueColumn( assignment );
		builder.addNonKeyRestriction( restriction );
		return builder.buildMutation();
	}

	RestrictedTableMutation<JdbcMutationOperation> buildRemoveAll(
			BasicCollectionPersister persister, MutatingTableReference table) {
		final int keyColumnCount = persister.getAttributeMapping().getKeyDescriptor().getJdbcTypeCount();
		final var parameters = new ColumnValueParameterList( table, ParameterUsage.RESTRICT, keyColumnCount );
		final var keys = new ArrayList<ColumnValueBinding>( keyColumnCount );
		persister.applyKeyRestrictions( parameters, keys );
		return new TableUpdateStandard( table, persister, comment, List.of( assignment ), keys, List.of( restriction ),
				null, removeAllExpectation, List.of() );
	}
}
