package org.hibernate.persister.entity.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hibernate.Internal;
import org.hibernate.StaleObjectStateException;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderSupport;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.model.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.TableMapping.MutationDetails;

import static java.util.Arrays.fill;
import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;

/// The value of a partition key column of an entity, used to restrict the
/// statements which update or delete its row.
@Internal
public record PartitionKeyValue(String table, SelectableMapping selectable, Object value) {
	private static final Predicate<AttributeMapping> EVERY_ATTRIBUTE = attribute -> true;

	/// Whether a statement with a parameter for each partition key column cannot be
	/// used, because the value of a column is null, or is unknown.
	public static boolean needsDynamicRestriction(EntityPersister persister, List<PartitionKeyValue> values) {
		return needsDynamicRestriction( persister, values, EVERY_ATTRIBUTE );
	}

	/// @param restricting the partition key attributes by which the row is restricted
	public static boolean needsDynamicRestriction(
			EntityPersister persister,
			List<PartitionKeyValue> values,
			Predicate<AttributeMapping> restricting) {
		for ( var key : values ) {
			if ( key.value == null ) {
				return true;
			}
		}
		return values.size() < columnCount( persister, restricting );
	}

	/// The values of the partition key columns in the given state of an entity, which
	/// is its state when it was loaded. The value of a column is unknown, and so left
	/// out, when there is no such state, or the attribute was never fetched.
	public static List<PartitionKeyValue> fromState(
			EntityPersister persister,
			Object[] state,
			SharedSessionContractImplementor session) {
		return fromState( persister, state, EVERY_ATTRIBUTE, session );
	}

	/// @param restricting the partition key attributes by which the row is restricted
	public static List<PartitionKeyValue> fromState(
			EntityPersister persister,
			Object[] state,
			Predicate<AttributeMapping> restricting,
			SharedSessionContractImplementor session) {
		if ( state == null || !persister.hasPartitionedSelectionMapping() ) {
			return List.of();
		}
		final var values = new ArrayList<PartitionKeyValue>();
		final var attributes = persister.getAttributeMappings();
		for ( int i = 0; i < attributes.size(); i++ ) {
			final var attribute = attributes.get( i );
			final Object attributeValue = state[attribute.getStateArrayPosition()];
			if ( attributeValue == UNFETCHED_PROPERTY || !restricting.test( attribute ) ) {
				continue;
			}
			for ( int j = 0; j < attribute.getJdbcTypeCount(); j++ ) {
				if ( isPartitionKey( attribute.getSelectable( j ) ) ) {
					attribute.decompose(
							attributeValue,
							(index, value, selectable) -> {
								if ( isPartitionKey( selectable ) ) {
									values.add( new PartitionKeyValue(
											persister.physicalTableNameForMutation( selectable ), selectable, value
									) );
								}
							},
							session
					);
					break;
				}
			}
		}
		return values;
	}

	public static List<PartitionKeyValue> forDelete(
			EntityPersister persister,
			Object identifier,
			Object[] loadedState,
			Set<String> customDeleteTables,
			SharedSessionContractImplementor session) {
		if ( customDeleteTables.isEmpty() ) {
			return fromState( persister, loadedState, session );
		}
		final var missingAttributes = new ArrayList<AttributeMapping>();
		final var attributes = persister.getAttributeMappings();
		for ( int i = 0; i < attributes.size(); i++ ) {
			final var attribute = attributes.get( i );
			if ( loadedState == null || loadedState[attribute.getStateArrayPosition()] == UNFETCHED_PROPERTY ) {
				for ( int j = 0; j < attribute.getJdbcTypeCount(); j++ ) {
					final var selectable = attribute.getSelectable( j );
					if ( isPartitionKey( selectable )
							&& customDeleteTables.contains( persister.physicalTableNameForMutation( selectable ) ) ) {
						missingAttributes.add( attribute );
						break;
					}
				}
			}
		}
		if ( missingAttributes.isEmpty() ) {
			return fromState( persister, loadedState, session );
		}

		// Custom SQL has a fixed parameter list, so its missing keys must come from the stored row.
		final var snapshot = SingleIdEntityLoaderSupport.loadAttributeSnapshot( persister, identifier, missingAttributes, session );
		if ( snapshot == null ) {
			throw new StaleObjectStateException( persister.getEntityName(), identifier );
		}
		final Object[] partitionState;
		if ( loadedState == null ) {
			partitionState = new Object[persister.getPropertySpan()];
			fill( partitionState, UNFETCHED_PROPERTY );
		}
		else {
			partitionState = loadedState.clone();
		}
		for ( int i = 0; i < missingAttributes.size(); i++ ) {
			partitionState[missingAttributes.get( i ).getStateArrayPosition()] = snapshot[i];
		}
		return fromState( persister, partitionState, session );
	}

	public static void addRestrictions(
			EntityPersister persister,
			Function<String, ? extends RestrictedTableMutationBuilder<?, ?>> builders,
			List<PartitionKeyValue> values,
			Function<TableMapping, MutationDetails> mutationDetails) {
		addRestrictions( persister, builders, values, mutationDetails, EVERY_ATTRIBUTE );
	}

	/// @param restricting the partition key attributes by which the row is restricted
	public static void addRestrictions(
			EntityPersister persister,
			Function<String, ? extends RestrictedTableMutationBuilder<?, ?>> builders,
			List<PartitionKeyValue> values,
			Function<TableMapping, MutationDetails> mutationDetails,
			Predicate<AttributeMapping> restricting) {
		if ( values == null ) {
			// The static statement has a parameter for each partition key column.
			final var attributes = persister.getAttributeMappings();
			for ( int i = 0; i < attributes.size(); i++ ) {
				if ( !restricting.test( attributes.get( i ) ) ) {
					continue;
				}
				attributes.get( i ).forEachSelectable( (index, selectable) -> {
					if ( isPartitionKey( selectable ) ) {
						final var builder = builders.apply( persister.physicalTableNameForMutation( selectable ) );
						if ( builder != null ) {
							builder.addKeyRestrictionLeniently( selectable );
						}
					}
				} );
			}
		}
		else {
			for ( var key : values ) {
				final var builder = builders.apply( key.table );
				if ( builder != null ) {
					final var details = mutationDetails.apply( builder.getMutatingTable().getTableMapping() );
					if ( key.value == null && details.getCustomSql() == null ) {
						builder.addNullRestriction( key.selectable );
					}
					else {
						builder.addKeyRestrictionLeniently( key.selectable );
					}
				}
			}
		}
	}

	public static void bindRestrictions(
			List<PartitionKeyValue> values,
			String table,
			JdbcValueBindings bindings,
			MutationOperation operation) {
		for ( var key : values ) {
			if ( key.table.equals( table ) ) {
				final var column = key.selectable.getSelectionExpression();
				// IS NULL has no parameter. Custom SQL can still have one for a null value.
				if ( key.value != null || operation.findValueDescriptor( column, ParameterUsage.RESTRICT ) != null ) {
					bindings.bindValue( key.value, column, ParameterUsage.RESTRICT );
				}
			}
		}
	}

	/// Bind the restrictions of the operations executed by a
	/// [org.hibernate.engine.jdbc.mutation.MutationExecutor].
	///
	/// @param operations the operation which mutates the table with a given name, if any
	public static void bindRestrictions(
			List<PartitionKeyValue> values,
			org.hibernate.engine.jdbc.mutation.JdbcValueBindings bindings,
			Function<String, ? extends MutationOperation> operations) {
		for ( var key : values ) {
			final var operation = operations.apply( key.table );
			if ( operation != null ) {
				final var column = key.selectable.getSelectionExpression();
				// IS NULL has no parameter. Custom SQL can still have one for a null value.
				if ( key.value != null || operation.findValueDescriptor( column, ParameterUsage.RESTRICT ) != null ) {
					bindings.bindValue( key.value, key.table, column, ParameterUsage.RESTRICT );
				}
			}
		}
	}

	/// Bind the restrictions of an operation which mutates a table with the
	/// partition key columns of the given table of the entity, executed by a
	/// [org.hibernate.engine.jdbc.mutation.MutationExecutor].
	public static void bindRestrictions(
			List<PartitionKeyValue> values,
			String table,
			org.hibernate.engine.jdbc.mutation.JdbcValueBindings bindings,
			MutationOperation operation) {
		for ( var key : values ) {
			if ( key.table.equals( table ) ) {
				final var column = key.selectable.getSelectionExpression();
				// IS NULL has no parameter. Custom SQL can still have one for a null value.
				if ( key.value != null || operation.findValueDescriptor( column, ParameterUsage.RESTRICT ) != null ) {
					bindings.bindValue( key.value, operation.getTableDetails().getTableName(), column,
							ParameterUsage.RESTRICT );
				}
			}
		}
	}

	private static int columnCount(EntityPersister persister, Predicate<AttributeMapping> restricting) {
		if ( !persister.hasPartitionedSelectionMapping() ) {
			return 0;
		}
		int count = 0;
		final var attributes = persister.getAttributeMappings();
		for ( int i = 0; i < attributes.size(); i++ ) {
			final var attribute = attributes.get( i );
			if ( restricting.test( attribute ) ) {
				for ( int j = 0; j < attribute.getJdbcTypeCount(); j++ ) {
					if ( isPartitionKey( attribute.getSelectable( j ) ) ) {
						count++;
					}
				}
			}
		}
		return count;
	}

	private static boolean isPartitionKey(SelectableMapping selectable) {
		return selectable.isPartitioned() && !selectable.isFormula();
	}
}
