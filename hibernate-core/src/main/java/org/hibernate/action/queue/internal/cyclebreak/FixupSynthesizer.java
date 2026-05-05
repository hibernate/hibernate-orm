/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.cyclebreak;

import org.hibernate.action.queue.spi.plan.FlushOperation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Factory for building cycle break fixup operations.
///
/// See [FkFixupUpdateFactory].
/// See [UniqueSwapUpdateFactory].
///
/// @author Steve Ebersole
@Incubating
public class FixupSynthesizer {
	private final FkFixupUpdateFactory nullableFkUpdateFactory;
	private final UniqueSwapUpdateFactory uniqueSwapUpdateFactory;

	public FixupSynthesizer() {
		this( new FkFixupUpdateFactory(), new UniqueSwapUpdateFactory() );
	}

	public FixupSynthesizer(FkFixupUpdateFactory nullableFkUpdateFactory, UniqueSwapUpdateFactory uniqueSwapUpdateFactory) {
		this.nullableFkUpdateFactory = nullableFkUpdateFactory;
		this.uniqueSwapUpdateFactory = uniqueSwapUpdateFactory;
	}

	/// Build the fix up operation (which is always an update), if one is needed, for
	/// the initial `cycleBrokenOp` FlushOperation.
	@Nullable
	public FlushOperation synthesizeFixupOperationIfNeeded(
			FlushOperation cycleBrokenOp,
			Object entityId,
			SharedSessionContractImplementor session) {
		final BindingPatch bindingPatch = cycleBrokenOp.getBindingPatch();
		if ( bindingPatch == null ) {
			throw new IllegalStateException( "No binding patch available" );
		}

		if ( bindingPatch.cycleType() == BindingPatch.CycleType.FOREIGN_KEY ) {
			return nullableFkUpdateFactory.buildOperationIfNeeded( cycleBrokenOp, entityId, session );
		}
		else {
			assert bindingPatch.cycleType() == BindingPatch.CycleType.UNIQUE_SWAP;
			return uniqueSwapUpdateFactory.buildOperationIfNeeded( cycleBrokenOp, entityId, session );
		}
	}
}
