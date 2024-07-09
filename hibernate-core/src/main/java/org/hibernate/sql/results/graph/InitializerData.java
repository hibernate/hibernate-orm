/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.graph;

import java.util.Objects;

import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;

public abstract class InitializerData {
	protected final RowProcessingState rowProcessingState;
	protected Initializer.State state = Initializer.State.UNINITIALIZED;
	protected @Nullable Object instance;

	public InitializerData(RowProcessingState rowProcessingState) {
		this.rowProcessingState = rowProcessingState;
	}

	/*
	 * Used by Hibernate Reactive
	 */
	public InitializerData(InitializerData original) {
		requireNonNull( original );
		this.rowProcessingState = original.rowProcessingState;
		this.state = original.state;
		this.instance = original.instance;
	}

	public RowProcessingState getRowProcessingState() {
		return rowProcessingState;
	}

	public Initializer.State getState() {
		return state;
	}

	public void setState(Initializer.State state) {
		this.state = state;
	}

	public @Nullable Object getInstance() {
		return instance;
	}

	public void setInstance(@Nullable Object instance) {
		this.instance = instance;
	}
}
