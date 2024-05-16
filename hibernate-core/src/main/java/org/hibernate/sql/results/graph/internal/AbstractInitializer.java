/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.internal;

import java.util.function.BiConsumer;

import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

public abstract class AbstractInitializer implements Initializer {

	protected RowProcessingState rowProcessingState;
	protected State state = State.UNINITIALIZED;

	@Override
	public void startLoading(RowProcessingState rowProcessingState) {
		this.rowProcessingState = rowProcessingState;
		forEachSubInitializer( Initializer::startLoading, rowProcessingState );
	}

	@Override
	public void endLoading(ExecutionContext executionContext) {
		rowProcessingState = null;
	}

	@Override
	public void resolveKey() {
		state = State.KEY_RESOLVED;
		forEachSubInitializer( (initializer, processingState) -> initializer.resolveKey(), rowProcessingState );
	}

	@Override
	public void initializeInstance() {
		// No-op by default
	}

	@Override
	public State getState() {
		return state;
	}

	@Override
	public void finishUpRow() {
		state = State.UNINITIALIZED;
	}

	protected abstract <X> void forEachSubInitializer(BiConsumer<Initializer, X> consumer, X arg);

}
