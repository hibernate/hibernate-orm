/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Spliterator;
import java.util.function.Consumer;

import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.model.domain.spi.InheritanceCapable;
import org.hibernate.metamodel.model.domain.spi.Navigable;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNavigableSpliterator<T extends Navigable> implements Spliterator<T> {
	public static final int CHARACTERISTICS = ORDERED
			& SORTED
			& DISTINCT
			// todo (6.0) : see note in `#navigableStream` - & Spliterator.SIZED
			& NONNULL
			& IMMUTABLE;

	private final Stack<InheritanceCapable> topDownHierarchy;

	private Spliterator<T> currentSpliterator;

	private boolean reachedEnd;

	public <T> AbstractNavigableSpliterator(InheritanceCapable container, boolean includeSuperNavigables) {
		this.topDownHierarchy = new Stack<>();

		addToStack( container, includeSuperNavigables );
	}

	private <T> void addToStack(InheritanceCapable container, boolean includeSuperNavigables) {
		final InheritanceCapable superType = container.getSuperclassType();
		if ( includeSuperNavigables && superType != null ) {
			addToStack( superType, includeSuperNavigables );
		}
		topDownHierarchy.push( container );
	}


	protected abstract Spliterator<T> nextSpliterator(InheritanceCapable container);

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		if ( reachedEnd ) {
			return false;
		}

		if ( currentSpliterator == null || !currentSpliterator.tryAdvance( action ) ) {
			currentSpliterator = advanceSpliterator( action );
			if ( topDownHierarchy.isEmpty() ) {
				reachedEnd = true;
				return false;
			}

			currentSpliterator = nextSpliterator( topDownHierarchy.pop() );

			if ( currentSpliterator == null ) {
				reachedEnd = true;
				return false;
			}
		}

		return true;
	}

	private Spliterator<T> advanceSpliterator(Consumer<? super T> action) {
		if ( topDownHierarchy.isEmpty() ) {
			return null;
		}

		final Spliterator<T> next = nextSpliterator( topDownHierarchy.pop() );
		if ( ! next.tryAdvance( action ) ) {
			return advanceSpliterator( action );
		}

		return next;
	}

	@Override
	public Spliterator<T> trySplit() {
		return currentSpliterator;
	}

	@Override
	public long estimateSize() {
		// todo (6.0) : the container should be able to pass this to us eventually
		//		the idea being that we could calculate and store this as part
		//		of finalizing the runtime model.  at that time the hierarchy would
		// 		know all of its declared attributes at each level.
		return 0;
	}

	@Override
	public int characteristics() {
		return CHARACTERISTICS;
	}
}
