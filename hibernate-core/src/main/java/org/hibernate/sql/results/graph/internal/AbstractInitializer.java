/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.internal;

import java.util.function.BiConsumer;

import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

public abstract class AbstractInitializer<Data extends InitializerData> implements Initializer<Data> {

	protected final int initializerId;

	protected AbstractInitializer(AssemblerCreationState creationState) {
		this.initializerId = creationState.acquireInitializerId();
	}

	@Override
	public void startLoading(RowProcessingState rowProcessingState) {
		final var data = createInitializerData( rowProcessingState );
		rowProcessingState.setInitializerData( initializerId, data );
		forEachSubInitializer( Initializer::startLoading, data );
	}

	protected abstract InitializerData createInitializerData(RowProcessingState rowProcessingState);

	@Override
	public void resolveKey(Data data) {
		data.setState( State.KEY_RESOLVED );
		forEachSubInitializer( Initializer::resolveKey, data );
	}

	@Override
	public void initializeInstance(Data data) {
		// No-op by default
	}

	@Override
	public Data getData(RowProcessingState rowProcessingState) {
		return rowProcessingState.getInitializerData( initializerId );
	}

	@Override
	public void finishUpRow(Data data) {
		data.setState( Initializer.State.UNINITIALIZED );
	}

	protected abstract void forEachSubInitializer(
			BiConsumer<Initializer<?>, RowProcessingState> consumer,
			InitializerData data);
}
