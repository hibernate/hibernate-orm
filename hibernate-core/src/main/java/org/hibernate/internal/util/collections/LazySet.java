/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.emptySet;

/**
 * @author Steve Ebersole
 */
public class LazySet<T> {
	private final Supplier<Set<T>> setBuilder;
	private Set<T> set;

	public LazySet() {
		this( HashSet::new );
	}

	public LazySet(Supplier<Set<T>> setBuilder) {
		this.setBuilder = setBuilder;
	}

	public void add(T element) {
		if ( set == null ) {
			set = setBuilder.get();
		}
		set.add( element );
	}

	public void forEach(Consumer<T> action) {
		if ( set == null ) {
			return;
		}
		set.forEach( action );
	}

	public Set<T> getUnderlyingSet() {
		return set == null ? emptySet() : set;
	}
}
