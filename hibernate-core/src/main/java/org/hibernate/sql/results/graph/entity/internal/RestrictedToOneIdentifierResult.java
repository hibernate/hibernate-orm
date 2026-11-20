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
 * Resolves the identifier selected by a restricted to-one join, while retaining the physical key.
 * The physical key need not be unique without the association's restrictions. The result keeps
 * the association path and result variable of the physical key.
 */
public final class RestrictedToOneIdentifierResult<T> implements DomainResult<T> {
	private final DomainResult<?> physicalKey;
	private final DomainResult<T> identifier;

	public RestrictedToOneIdentifierResult(DomainResult<?> physicalKey, DomainResult<T> identifier) {
		this.physicalKey = physicalKey;
		this.identifier = identifier;
	}

	@Override
	public String getResultVariable() {
		return physicalKey.getResultVariable();
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return identifier.getResultJavaType();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return physicalKey.getNavigablePath();
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return physicalKey.containsAnyNonScalarResults() || identifier.containsAnyNonScalarResults();
	}

	@Override
	public void collectValueIndexesToCache(BitSet indexes) {
		if ( physicalKey != identifier ) {
			physicalKey.collectValueIndexesToCache( indexes );
		}
		identifier.collectValueIndexesToCache( indexes );
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(InitializerParent<?> parent, AssemblerCreationState state) {
		if ( physicalKey == identifier ) {
			final var assembler = identifier.createResultAssembler( parent, state );
			return new Assembler<>( assembler, assembler );
		}
		return new Assembler<>( physicalKey.createResultAssembler( parent, state ),
				identifier.createResultAssembler( parent, state ) );
	}

	public record Assembler<T>(DomainResultAssembler<?> physicalKey, DomainResultAssembler<T> identifier)
			implements DomainResultAssembler<T> {
		@Override
		public T assemble(RowProcessingState state) {
			return identifier.assemble( state );
		}

		public Object getFilteredKey(RowProcessingState state) {
			return physicalKey != identifier && identifier.assemble( state ) == null ? physicalKey.assemble( state ) : null;
		}

		@Override
		public JavaType<T> getAssembledJavaType() {
			return identifier.getAssembledJavaType();
		}

		@Override
		public Initializer<?> getInitializer() {
			return identifier.getInitializer();
		}

		@Override
		public void resolveState(RowProcessingState state) {
			if ( physicalKey != identifier ) {
				physicalKey.resolveState( state );
			}
			identifier.resolveState( state );
		}

		public void forEachInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, RowProcessingState rowProcessingState) {
			if ( physicalKey.getInitializer() != null ) {
				consumer.accept( physicalKey.getInitializer(), rowProcessingState );
			}
			if ( identifier.getInitializer() != null && identifier.getInitializer() != physicalKey.getInitializer() ) {
				consumer.accept( identifier.getInitializer(), rowProcessingState );
			}
		}

		@Override
		public <X> void forEachResultAssembler(BiConsumer<Initializer<?>, X> consumer, X arg) {
			if ( physicalKey != identifier ) {
				physicalKey.forEachResultAssembler( consumer, arg );
			}
			identifier.forEachResultAssembler( consumer, arg );
		}
	}
}
