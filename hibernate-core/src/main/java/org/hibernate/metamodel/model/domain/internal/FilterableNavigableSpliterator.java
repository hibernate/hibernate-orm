/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;
import java.util.Spliterator;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.metamodel.model.domain.spi.InheritanceCapable;
import org.hibernate.metamodel.model.domain.spi.Navigable;

/**
 * @author Steve Ebersole
 */
public class FilterableNavigableSpliterator<N extends Navigable<?>> implements Spliterator<N> {
	private final Stack<List<N>> topDownNavListStack;
	private final int size;

	private Spliterator<N> currentSubSpliterator;
	private boolean reachedEnd;

	public FilterableNavigableSpliterator(
			InheritanceCapable container,
			Class<N> filterType,
			boolean includeSuperNavigables) {
		this.topDownNavListStack = new Stack<>();

		this.size = addToStack( container, filterType, includeSuperNavigables );
	}

	private int addToStack(
			InheritanceCapable<?> container,
			Class<N> filterType,
			boolean includeSuperNavigables) {
		final List<N> filteredNavigables = filterNavigables( container, filterType );
		int count = filteredNavigables.size();

		topDownNavListStack.push( filteredNavigables );

		final InheritanceCapable<?> superType = container.getSuperclassType();
		if ( includeSuperNavigables && superType != null ) {
			count+= addToStack( superType, filterType, true );
		}

		return count;
	}

	/**
	 * Protected-access to allow sub-types to potentially define better
	 * filtering algorithm
	 */
	@SuppressWarnings({"WeakerAccess", "unchecked"})
	protected List<N> filterNavigables(InheritanceCapable<?> container, Class<N> filterType) {
		final List<N> unfiltered = (List<N>) container.getDeclaredNavigables();
		if ( filterType == null || Navigable.class.equals( filterType ) ) {
			return unfiltered;
		}

		return unfiltered.stream()
				.filter( filterType::isInstance )
				.map( filterType::cast )
				.collect( Collectors.toList() );
	}

	@Override
	public boolean tryAdvance(Consumer<? super N> action) {
		System.out.println("try advance");
		if ( reachedEnd ) {
			// should indicate we are past the end of all sub-spliterators
			return false;
		}

		if ( currentSubSpliterator == null ) {
			// should indicate the first pass
			currentSubSpliterator = nextSubSpliterator();

			if ( currentSubSpliterator == null ) {
				return false;
			}
		}

		return internalTryAdvance( action );
	}

	private Spliterator<N> nextSubSpliterator() {
		if ( topDownNavListStack.isEmpty() ) {
			reachedEnd = true;
			return null;
		}

		System.out.println( "Popping Stack element for next Spliterator" );

		return topDownNavListStack.pop().spliterator();
	}

	@SuppressWarnings("PointlessBooleanExpression")
	private boolean internalTryAdvance(Consumer<? super N> action) {
		boolean reply = currentSubSpliterator.tryAdvance( action );

		if ( reply == false ) {
			while ( !reachedEnd && reply == false ) {
				// see if there is another Spliterator left
				currentSubSpliterator = nextSubSpliterator();
				if ( currentSubSpliterator == null ) {
					assert reachedEnd;
					return false;
				}
				reply = currentSubSpliterator.tryAdvance( action );
			}
		}

		return reply;
	}

	@Override
	public Spliterator<N> trySplit() {
		System.out.println("try split");
		if ( topDownNavListStack.isEmpty() ) {
			return null;
		}

		return nextSubSpliterator();
	}

	@Override
	public long estimateSize() {
		return size;
	}

	@Override
	public long getExactSizeIfKnown() {
		return size;
	}

	public static final int CHARACTERISTICS = SIZED
			& SORTED
			& DISTINCT
			& NONNULL
			& IMMUTABLE;

	@Override
	public int characteristics() {
		return CHARACTERISTICS;
	}
}
