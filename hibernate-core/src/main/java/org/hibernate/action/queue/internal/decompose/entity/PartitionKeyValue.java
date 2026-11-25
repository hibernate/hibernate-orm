package org.hibernate.action.queue.internal.decompose.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.model.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.TableMapping.MutationDetails;

record PartitionKeyValue(String table, SelectableMapping selectable, Object value) {
	static boolean hasNull(List<PartitionKeyValue> values) {
		for ( var key : values ) {
			if ( key.value == null ) {
				return true;
			}
		}
		return false;
	}

	static List<PartitionKeyValue> fromState(
			EntityPersister persister,
			Object[] state,
			SharedSessionContractImplementor session) {
		if ( state == null || !persister.hasPartitionedSelectionMapping() ) {
			return List.of();
		}
		final var values = new ArrayList<PartitionKeyValue>();
		final var attributes = persister.getAttributeMappings();
		for ( int i = 0; i < attributes.size(); i++ ) {
			final var attribute = attributes.get( i );
			for ( int j = 0; j < attribute.getJdbcTypeCount(); j++ ) {
				if ( isPartitionKey( attribute.getSelectable( j ) ) ) {
					attribute.decompose(
							state[attribute.getStateArrayPosition()],
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

	static void addRestrictions(
			EntityPersister persister,
			Map<String, ? extends RestrictedTableMutationBuilder<?, ?>> builders,
			List<PartitionKeyValue> values,
			Function<TableMapping, MutationDetails> mutationDetails) {
		if ( values == null ) {
			// The static statement has a parameter for each partition key column.
			final var attributes = persister.getAttributeMappings();
			for ( int i = 0; i < attributes.size(); i++ ) {
				attributes.get( i ).forEachSelectable( (index, selectable) -> {
					if ( isPartitionKey( selectable ) ) {
						final var builder = builders.get( persister.physicalTableNameForMutation( selectable ) );
						if ( builder != null ) {
							builder.addKeyRestrictionLeniently( selectable );
						}
					}
				} );
			}
		}
		else {
			for ( var key : values ) {
				final var builder = builders.get( key.table );
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

	static void bindRestrictions(
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

	private static boolean isPartitionKey(SelectableMapping selectable) {
		return selectable.isPartitioned() && !selectable.isFormula();
	}
}
