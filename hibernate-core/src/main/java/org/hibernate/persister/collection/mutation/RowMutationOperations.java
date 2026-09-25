package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;

import static org.hibernate.internal.util.NullnessHelper.areSameNullness;

/**
 * Composition of the {@link MutationOperation} references for a collection mapping.
 *
 * @implSpec All collection operations are achieved through {@link JdbcMutationOperation}
 * which is exposed here
 *
 * @author Steve Ebersole
 */

public class RowMutationOperations {
	@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
	public static final ModelPart.JdbcValueBiConsumer<JdbcValueBindings, Object> DEFAULT_RESTRICTOR = (valueIndex, jdbcValueBindings, o, value, jdbcValueMapping) -> {
		jdbcValueBindings.bindValue( value, jdbcValueMapping, ParameterUsage.RESTRICT );
	};
	@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
	public static final ModelPart.JdbcValueBiConsumer<JdbcValueBindings, Object> DEFAULT_VALUE_SETTER = (valueIndex, jdbcValueBindings, o, value, jdbcValueMapping) -> {
		jdbcValueBindings.bindValue( value, jdbcValueMapping, ParameterUsage.SET );
	};
	private final CollectionMutationTarget target;

	@Nullable
	private final OperationProducer insertRowOperationProducer;
	@Nullable
	private final Values insertRowValues;

	@Nullable
	private final OperationProducer updateRowOperationProducer;
	@Nullable
	private final Values updateRowValues;
	@Nullable
	private final Restrictions updateRowRestrictions;

	@Nullable
	private final OperationProducer deleteRowOperationProducer;
	@Nullable
	private final Restrictions deleteRowRestrictions;

	@Nullable
	private final OperationProducer deleteAllRowsOperationProducer;

	@Nullable
	private JdbcMutationOperation insertRowOperation;
	@Nullable
	private JdbcMutationOperation updateRowOperation;
	@Nullable
	private JdbcMutationOperation deleteRowOperation;

	public RowMutationOperations(
			@Nonnull CollectionMutationTarget target,
			@Nullable OperationProducer insertRowOperationProducer,
			@Nullable Values insertRowValues,
			@Nullable OperationProducer updateRowOperationProducer,
			@Nullable Values updateRowValues,
			@Nullable Restrictions updateRowRestrictions,
			@Nullable OperationProducer deleteRowOperationProducer,
			@Nullable Restrictions deleteRowRestrictions,
			@Nullable OperationProducer deleteAllRowsOperationProducer) {
		this.target = target;

		assert areSameNullness( insertRowOperationProducer, insertRowValues );
		assert areSameNullness( updateRowOperationProducer, updateRowValues, updateRowRestrictions );
		assert areSameNullness( deleteRowOperationProducer, deleteRowRestrictions );

		this.insertRowOperationProducer = insertRowOperationProducer;
		this.insertRowValues = insertRowValues;

		this.updateRowOperationProducer = updateRowOperationProducer;
		this.updateRowValues = updateRowValues;
		this.updateRowRestrictions = updateRowRestrictions;

		this.deleteRowOperationProducer = deleteRowOperationProducer;
		this.deleteRowRestrictions = deleteRowRestrictions;

		this.deleteAllRowsOperationProducer = deleteAllRowsOperationProducer;
	}

	@Nonnull
	@Override
	public String toString() {
		return "RowMutationOperations(" + target.getRolePath() + ")";
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// insert row

	public boolean hasInsertRow() {
		return insertRowOperationProducer != null;
	}

	@Nullable
	public Values getInsertRowValues() {
		return insertRowValues;
	}

	@Nullable
	public JdbcMutationOperation getInsertRowOperation() {
		if ( insertRowOperationProducer == null ) {
			return null;
		}
		else {
			JdbcMutationOperation local = insertRowOperation;
			if ( local == null ) {
				final var tableReference = new MutatingTableReference( target.getCollectionTableMapping() );
				insertRowOperation = local = insertRowOperationProducer.createOperation( tableReference );
			}
			return local;
		}
	}

	@Nullable
	public JdbcMutationOperation getInsertRowOperation(@Nonnull TableMapping tableMapping) {
		if ( insertRowOperationProducer == null ) {
			return null;
		}
		else {
			final var tableReference = new MutatingTableReference( tableMapping );
			return insertRowOperationProducer.createOperation( tableReference );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// update row

	public boolean hasUpdateRow() {
		return updateRowOperationProducer != null;
	}

	@Nullable
	public JdbcMutationOperation getUpdateRowOperation() {
		if ( updateRowOperationProducer == null ) {
			return null;
		}
		else {
			JdbcMutationOperation local = updateRowOperation;
			if ( local == null ) {
				final var tableReference = new MutatingTableReference( target.getCollectionTableMapping() );
				updateRowOperation = local = updateRowOperationProducer.createOperation( tableReference );
			}
			return local;
		}
	}

	@Nullable
	public Values getUpdateRowValues() {
		return updateRowValues;
	}

	@Nullable
	public Restrictions getUpdateRowRestrictions() {
		return updateRowRestrictions;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// delete row

	public boolean hasDeleteRow() {
		return deleteRowOperationProducer != null;
	}

	@Nullable
	public Restrictions getDeleteRowRestrictions() {
		return deleteRowRestrictions;
	}

	@Nullable
	public JdbcMutationOperation getDeleteRowOperation() {
		if ( deleteRowOperationProducer == null ) {
			return null;
		}
		else {
			JdbcMutationOperation local = deleteRowOperation;
			if ( local == null ) {
				final var tableReference = new MutatingTableReference( target.getCollectionTableMapping() );
				deleteRowOperation = local = deleteRowOperationProducer.createOperation( tableReference );
			}
			return local;
		}
	}

	@Nullable
	public JdbcMutationOperation getDeleteRowOperation(@Nonnull TableMapping tableMapping) {
		if ( deleteRowOperationProducer == null ) {
			return null;
		}
		else {
			final var tableReference = new MutatingTableReference( tableMapping );
			return deleteRowOperationProducer.createOperation( tableReference );
		}
	}

	@Nullable
	public OperationProducer getDeleteAllRowsOperationProducer() {
		return deleteAllRowsOperationProducer;
	}

	@FunctionalInterface
	public interface Restrictions {
		void applyRestrictions(
				@Nonnull PersistentCollection<?> collection,
				@Nonnull Object key,
				@Nonnull Object rowValue,
				int rowPosition,
				@Nonnull SharedSessionContractImplementor session,
				@Nonnull JdbcValueBindings jdbcValueBindings);
	}

	@FunctionalInterface
	public interface Values {
		void applyValues(
				@Nonnull PersistentCollection<?> collection,
				@Nonnull Object key,
				@Nonnull Object rowValue,
				int rowPosition,
				@Nonnull SharedSessionContractImplementor session,
				@Nonnull JdbcValueBindings jdbcValueBindings);
	}

}
