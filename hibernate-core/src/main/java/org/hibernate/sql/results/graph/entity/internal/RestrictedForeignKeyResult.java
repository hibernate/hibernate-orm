package org.hibernate.sql.results.graph.entity.internal;

import java.util.BitSet;
import java.util.function.BiConsumer;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Resolves a reference only when the restricted target join matched, while retaining the physical key.
 */
public final class RestrictedForeignKeyResult<T> implements DomainResult<T> {
	private final DomainResult<T> key;
	private final DomainResult<?> target;

	public RestrictedForeignKeyResult(DomainResult<T> key, DomainResult<?> target) {
		this.key = key;
		this.target = target;
	}

	@Override
	public String getResultVariable() {
		return key.getResultVariable();
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return key.getResultJavaType();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return key.getNavigablePath();
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return key.containsAnyNonScalarResults() || target.containsAnyNonScalarResults();
	}

	@Override
	public void collectValueIndexesToCache(BitSet indexes) {
		key.collectValueIndexesToCache( indexes );
		target.collectValueIndexesToCache( indexes );
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(InitializerParent<?> parent, AssemblerCreationState state) {
		return new Assembler<>( key.createResultAssembler( parent, state ), target.createResultAssembler( parent, state ) );
	}

	public record Assembler<T>(DomainResultAssembler<T> key, DomainResultAssembler<?> target)
			implements DomainResultAssembler<T> {
		@Override
		public T assemble(RowProcessingState state) {
			final T value = key.assemble( state );
			return target.assemble( state ) == null ? null : value;
		}

		public Object getFilteredKey(RowProcessingState state) {
			return target.assemble( state ) == null ? key.assemble( state ) : null;
		}

		@Override
		public JavaType<T> getAssembledJavaType() {
			return key.getAssembledJavaType();
		}

		@Override
		public Initializer<?> getInitializer() {
			return key.getInitializer();
		}

		@Override
		public void resolveState(RowProcessingState state) {
			key.resolveState( state );
			target.resolveState( state );
		}

		public void forEachInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, RowProcessingState rowProcessingState) {
			if ( key.getInitializer() != null ) {
				consumer.accept( key.getInitializer(), rowProcessingState );
			}
			if ( target.getInitializer() != null && target.getInitializer() != key.getInitializer() ) {
				consumer.accept( target.getInitializer(), rowProcessingState );
			}
		}

		@Override
		public <X> void forEachResultAssembler(BiConsumer<Initializer<?>, X> consumer, X arg) {
			key.forEachResultAssembler( consumer, arg );
			target.forEachResultAssembler( consumer, arg );
		}
	}
}
