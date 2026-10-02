package org.hibernate.dialect.lock.spi;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/// Factory for immutable concurrency declarations and dialect profiles.
/// Unspecified conflicts are UNKNOWN and unspecified read guarantees are false.
/// For the textual declarations accepted by the standard resolver, see
/// [org.hibernate.cfg.TransactionSettings#TRANSACTION_CONCURRENCY].
///
/// @since 8.0
/// @author Steve Ebersole
public final class TransactionConcurrencies {
	private TransactionConcurrencies() {
	}

	/// Start a declaration. READ and WRITE are supported by default.
	public static Builder builder(String name) {
		return new Builder( name );
	}

	/// Immutable read guarantees, suitable for a programmatic declaration.
	public record Guarantees(
			boolean preventsDirtyReads,
			boolean hasStableRowView,
			boolean preventsConcurrentModification,
			boolean holdsRowLockUntilTransactionCompletion,
			boolean isCurrentRead) implements ReadGuarantees {
	}

	/// Mutable builder; built profiles do not retain its state.
	public static final class Builder {
		private final String name;
		private final EnumMap<Operation, Guarantees> reads = new EnumMap<>( Operation.class );
		private final EnumMap<Operation, EnumMap<Operation, BlockingDuration>> conflicts =
				new EnumMap<>( Operation.class );

		private Builder(String name) {
			this.name = Objects.requireNonNull( name );
			reads.put( Operation.READ, new Guarantees( false, false, false, false, false ) );
		}

		/// Add an actual supported read operation and copy its guarantees.
		public Builder read(Operation operation, ReadGuarantees guarantees) {
			if ( operation == Operation.WRITE ) {
				throw new IllegalArgumentException( "WRITE is not a read" );
			}
			reads.put( Objects.requireNonNull( operation ), new Guarantees(
					guarantees.preventsDirtyReads(), guarantees.hasStableRowView(),
					guarantees.preventsConcurrentModification(),
					guarantees.holdsRowLockUntilTransactionCompletion(), guarantees.isCurrentRead() ) );
			return this;
		}

		/// Declare a directional conflict. Both operations must be supported at build time.
		public Builder blocking(Operation preceding, Operation concurrent, BlockingDuration duration) {
			conflicts.computeIfAbsent( Objects.requireNonNull( preceding ), ignored -> new EnumMap<>( Operation.class ) )
					.put( Objects.requireNonNull( concurrent ), Objects.requireNonNull( duration ) );
			return this;
		}

		/// Create an independent immutable snapshot.
		public TransactionConcurrency build() {
			final EnumMap<Operation, Map<Operation, BlockingDuration>> copy = new EnumMap<>( Operation.class );
			conflicts.forEach( (preceding, row) -> {
				checkSupported( preceding );
				row.keySet().forEach( this::checkSupported );
				copy.put( preceding, Map.copyOf( row ) );
			} );
			return new Profile( name, Map.copyOf( reads ), Map.copyOf( copy ) );
		}

		private void checkSupported(Operation operation) {
			if ( operation != Operation.WRITE && !reads.containsKey( operation ) ) {
				throw new IllegalArgumentException( "Unsupported operation: " + operation );
			}
		}
	}

	/// Immutable concurrency profile.
	private record Profile(
			String getName,
			Map<Operation, Guarantees> reads,
			Map<Operation, Map<Operation, BlockingDuration>> conflicts) implements TransactionConcurrency {
		@Override
		public boolean supports(Operation operation) {
			return Objects.requireNonNull( operation ) == Operation.WRITE || reads.containsKey( operation );
		}

		@Override
		public BlockingDuration getBlockingDuration(Operation preceding, Operation concurrent) {
			checkSupported( preceding );
			checkSupported( concurrent );
			return conflicts.getOrDefault( preceding, Map.of() ).getOrDefault( concurrent, BlockingDuration.UNKNOWN );
		}

		@Override
		public ReadGuarantees getReadGuarantees(Operation operation) {
			if ( operation == Operation.WRITE ) {
				throw new IllegalArgumentException( "WRITE is not a read" );
			}
			checkSupported( operation );
			return reads.get( operation );
		}

		private void checkSupported(Operation operation) {
			if ( !supports( operation ) ) {
				throw new UnsupportedOperationException( getName + " does not support " + operation );
			}
		}
	}
}
