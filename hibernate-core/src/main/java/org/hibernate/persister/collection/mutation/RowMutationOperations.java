/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

/**
 * Composition of the {@link MutationOperation} references for a collection mapping.
 *
 * @implSpec All collection operations are achieved through {@link JdbcMutationOperation}
 * which is exposed here
 *
 * @author Steve Ebersole
 */
public class RowMutationOperations {
	public static final ModelPart.JdbcValueBiConsumer<JdbcValueBindings, Object> DEFAULT_RESTRICTOR = (valueIndex, jdbcValueBindings, o, value, jdbcValueMapping) -> {
		jdbcValueBindings.bindValue( value, jdbcValueMapping, ParameterUsage.RESTRICT );
	};
	public static final ModelPart.JdbcValueBiConsumer<JdbcValueBindings, Object> DEFAULT_VALUE_SETTER = (valueIndex, jdbcValueBindings, o, value, jdbcValueMapping) -> {
		jdbcValueBindings.bindValue( value, jdbcValueMapping, ParameterUsage.SET );
	};
	private final CollectionMutationTarget target;

	private final OperationProducer insertRowOperationProducer;
	private final Values insertRowValues;

	private final OperationProducer updateRowOperationProducer;
	private final Values updateRowValues;
	private final Restrictions updateRowRestrictions;

	private final OperationProducer deleteRowOperationProducer;
	private final Restrictions deleteRowRestrictions;

	private JdbcMutationOperation insertRowOperation;
	private JdbcMutationOperation updateRowOperation;
	private JdbcMutationOperation deleteRowOperation;

	public RowMutationOperations(
			CollectionMutationTarget target,
			OperationProducer insertRowOperationProducer,
			Values insertRowValues,
			OperationProducer updateRowOperationProducer,
			Values updateRowValues,
			Restrictions updateRowRestrictions,
			OperationProducer deleteRowOperationProducer,
			Restrictions deleteRowRestrictions) {
		this.target = target;

		assert NullnessHelper.areSameNullness( insertRowOperationProducer, insertRowValues );
		assert NullnessHelper.areSameNullness( updateRowOperationProducer, updateRowValues, updateRowRestrictions );
		assert NullnessHelper.areSameNullness( deleteRowOperationProducer, deleteRowRestrictions );

		this.insertRowOperationProducer = insertRowOperationProducer;
		this.insertRowValues = insertRowValues;

		this.updateRowOperationProducer = updateRowOperationProducer;
		this.updateRowValues = updateRowValues;
		this.updateRowRestrictions = updateRowRestrictions;

		this.deleteRowOperationProducer = deleteRowOperationProducer;
		this.deleteRowRestrictions = deleteRowRestrictions;
	}

	@Override
	public String toString() {
		return "RowMutationOperations(" + target.getRolePath() + ")";
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// insert row

	public boolean hasInsertRow() {
		return insertRowOperationProducer != null;
	}

	public Values getInsertRowValues() {
		return insertRowValues;
	}

	public JdbcMutationOperation getInsertRowOperation() {
		if ( !hasInsertRow() ) {
			return null;
		}

		JdbcMutationOperation local = insertRowOperation;
		if ( local == null ) {
			final MutatingTableReference tableReference = new MutatingTableReference( target.getCollectionTableMapping() );
			insertRowOperation = local = insertRowOperationProducer.createOperation( tableReference );
		}

		return local;
	}

	public JdbcMutationOperation getInsertRowOperation(TableMapping tableMapping) {
		if ( !hasInsertRow() ) {
			return null;
		}

		final MutatingTableReference tableReference = new MutatingTableReference( tableMapping );
		return insertRowOperationProducer.createOperation( tableReference );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// update row

	public boolean hasUpdateRow() {
		return updateRowOperationProducer != null;
	}

	public JdbcMutationOperation getUpdateRowOperation() {
		if ( !hasUpdateRow() ) {
			return null;
		}

		JdbcMutationOperation local = updateRowOperation;
		if ( local == null ) {
			final MutatingTableReference tableReference = new MutatingTableReference( target.getCollectionTableMapping() );
			updateRowOperation = local = updateRowOperationProducer.createOperation( tableReference );
		}

		return local;
	}

	public Values getUpdateRowValues() {
		return updateRowValues;
	}

	public Restrictions getUpdateRowRestrictions() {
		return updateRowRestrictions;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// delete row

	public boolean hasDeleteRow() {
		return deleteRowOperationProducer != null;
	}

	public Restrictions getDeleteRowRestrictions() {
		return deleteRowRestrictions;
	}

	public JdbcMutationOperation getDeleteRowOperation() {
		if ( !hasDeleteRow() ) {
			return null;
		}

		JdbcMutationOperation local = deleteRowOperation;
		if ( local == null ) {
			final MutatingTableReference tableReference = new MutatingTableReference( target.getCollectionTableMapping() );
			deleteRowOperation = local = deleteRowOperationProducer.createOperation( tableReference );
		}

		return local;
	}

	public JdbcMutationOperation getDeleteRowOperation(TableMapping tableMapping) {
		if ( !hasInsertRow() ) {
			return null;
		}

		final MutatingTableReference tableReference = new MutatingTableReference( tableMapping );
		return deleteRowOperationProducer.createOperation( tableReference );
	}


	@FunctionalInterface
	public interface Restrictions {
		void applyRestrictions(
				PersistentCollection<?> collection,
				Object key,
				Object rowValue,
				int rowPosition,
				SharedSessionContractImplementor session,
				JdbcValueBindings jdbcValueBindings);
	}

	@FunctionalInterface
	public interface Values {
		void applyValues(
				PersistentCollection<?> collection,
				Object key,
				Object rowValue,
				int rowPosition,
				SharedSessionContractImplementor session,
				JdbcValueBindings jdbcValueBindings);
	}

}
