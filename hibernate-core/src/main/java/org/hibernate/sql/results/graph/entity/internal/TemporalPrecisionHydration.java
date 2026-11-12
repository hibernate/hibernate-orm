package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.internal.TemporalPrecisionBasicType;
import org.hibernate.type.internal.TemporalPreparedState;

/// Retains the extracted relational value before cache publication can lose it
/// behind an application converter. Embeddables recurse within the same owner;
/// associated entities maintain their own entries.
///
/// @author Steve Ebersole
public final class TemporalPrecisionHydration {
	private TemporalPrecisionHydration() {
	}

	public static void record(EntityEntry entry, ModelPart part, DomainResultAssembler<?> assembler, RowProcessingState row) {
		if ( entry != null && entry.getPersister().canWriteToCache() ) {
			TemporalPreparedState.attach( entry, capture( part, assembler, row ) );
		}
	}

	/// A row-scoped snapshot, taken before the normal read converter is invoked.
	public static TemporalPreparedState capture(ModelPart part, DomainResultAssembler<?> assembler, RowProcessingState row) {
		if ( assembler instanceof BasicResultAssembler<?> && part instanceof BasicValuedModelPart mapping
				&& TemporalPrecisionBasicType.find( mapping.getJdbcMapping() ) != null
				|| assembler != null && assembler.getInitializer() instanceof EmbeddableInitializer<?> ) {
			final var state = new TemporalPreparedState();
			record( state, part, assembler, row );
			return state.isEmpty() ? null : state;
		}
		return null;
	}

	public static void record(TemporalPreparedState state, ModelPart part, DomainResultAssembler<?> assembler, RowProcessingState row) {
		if ( assembler instanceof BasicResultAssembler<?> basic && part instanceof BasicValuedModelPart mapping ) {
			final var temporal = TemporalPrecisionBasicType.find( mapping.getJdbcMapping() );
			if ( temporal != null ) {
				state.record( temporal, basic.extractRawValue( row ), row.getSession() );
			}
		}
		else if ( assembler != null && assembler.getInitializer() instanceof EmbeddableInitializer<?> embedded ) {
			embedded.collectTemporalPreparedState( row, state );
		}
	}

}
