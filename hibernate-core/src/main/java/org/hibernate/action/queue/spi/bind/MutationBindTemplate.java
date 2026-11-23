package org.hibernate.action.queue.spi.bind;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.JdbcValueDescriptorImpl;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.ToIntFunction;

/// Ordered binding slots for a mutation operation.
///
/// A template is created from the ordered parameter binders of a
/// [PreparableMutationOperation] when every binder is a
/// [ColumnValueParameter].  In that case, [JdbcValueBindings] can bind values
/// by array slot instead of building descriptor-based [org.hibernate.engine.jdbc.mutation.spi.BindingGroup]
/// instances for each row.
///
/// Templates are cached per mutation operation.  Each template is immutable
/// after construction and may be reused by many per-row binding instances.
///
/// @author Steve Ebersole
public final class MutationBindTemplate {
	private static final Map<PreparableMutationOperation, MutationBindTemplate> TEMPLATE_CACHE = new WeakHashMap<>();

	private final BindSlot[] slots;
	private final int[] componentIndexes;
	private final Map<ParameterUsage, Map<String, BindSlot>> slotsByUsage;

	/// Resolve the slot template for the given mutation operation.
	///
	/// @return the cached template, or `null` when the operation contains a
	/// non-column-value parameter binder and must use descriptor-based binding
	public static MutationBindTemplate forOperation(PreparableMutationOperation operation) {
		if ( !hasColumnValueParameters( operation ) ) {
			return null;
		}
		synchronized ( TEMPLATE_CACHE ) {
			return TEMPLATE_CACHE.computeIfAbsent( operation, MutationBindTemplate::new );
		}
	}

	private static boolean hasColumnValueParameters(PreparableMutationOperation operation) {
		final var parameterBinders = operation.getParameterBinders();
		for ( int i = 0; i < parameterBinders.size(); i++ ) {
			if ( !( parameterBinders.get( i ) instanceof ColumnValueParameter ) ) {
				return false;
			}
		}
		return true;
	}

	private MutationBindTemplate(PreparableMutationOperation operation) {
		this( operation, null, null );
	}

	/// Map the physical parameters of a specialized statement back to this template's row storage.
	public MutationBindTemplate project(PreparableMutationOperation operation, ToIntFunction<ColumnValueParameter> componentIndex) {
		return new MutationBindTemplate( operation, this, componentIndex );
	}

	private MutationBindTemplate(PreparableMutationOperation operation, MutationBindTemplate source,
			ToIntFunction<ColumnValueParameter> componentIndex) {
		final int offset = operation.getExpectation().getNumberOfParametersUsed();
		final var parameterBinders = operation.getParameterBinders();
		this.slots = new BindSlot[parameterBinders.size()];
		this.componentIndexes = source == null ? null : new int[parameterBinders.size()];
		this.slotsByUsage = new EnumMap<>( ParameterUsage.class );

		for ( int i = 0; i < parameterBinders.size(); i++ ) {
			final var columnValueParameter = (ColumnValueParameter) parameterBinders.get( i );
			final var valueDescriptor = new JdbcValueDescriptorImpl( columnValueParameter, offset + i + 1 );
			final var slotsByColumn = slotsByUsage.computeIfAbsent( valueDescriptor.getUsage(), usage -> new HashMap<>() );
			final int index;
			if ( source == null ) {
				final var existing = slotsByColumn.get( valueDescriptor.getColumnName() );
				index = existing == null ? i : existing.index();
			}
			else {
				index = source.findSlot( valueDescriptor.getColumnName(), valueDescriptor.getUsage() ).index();
				componentIndexes[i] = componentIndex.applyAsInt( columnValueParameter );
			}
			final var slot = new BindSlot(
					index,
					valueDescriptor.getColumnName(),
					valueDescriptor.getUsage(),
					offset + i + 1,
					valueDescriptor.getJdbcMapping(),
					valueDescriptor
			);
			slots[i] = slot;
			slotsByColumn.putIfAbsent( slot.columnName(), slot );
		}
	}

	/// The mutation's ordered JDBC binding slots.
	///
	/// The template owns the array and orders it by JDBC parameter position.
	/// [BindSlot#index()] identifies the position in per-row value storage.
	/// Repeated parameters for the same column and usage share that position.
	public BindSlot[] slots() {
		return slots;
	}

	Object jdbcValue(int parameterIndex, Object value) {
		if ( componentIndexes == null || componentIndexes[parameterIndex] < 0 ) {
			return value;
		}
		return ( (Object[]) value )[componentIndexes[parameterIndex]];
	}

	/// Find the slot for a column and parameter usage.
	///
	/// @return the matching slot, or `null` if the mutation operation does not
	/// contain a parameter for the requested column and usage
	public BindSlot findSlot(String columnName, ParameterUsage usage) {
		final Map<String, BindSlot> slotsByColumn = slotsByUsage.get( usage );
		return slotsByColumn == null ? null : slotsByColumn.get( columnName );
	}
}
