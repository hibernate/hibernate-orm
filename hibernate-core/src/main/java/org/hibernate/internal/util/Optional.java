/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A slightly less-broken version of {@link java.util.Optional}.
 *
 * @author Gavin King
 */
sealed public interface Optional<T> {
	record Defined<T>(T result) implements Optional<T> {}
	record Undefined<T>() implements Optional<T> {}

	static <T> Undefined<T> undefined() {
		return new Undefined<>();
	}
	static <T> Defined<T> of(T t) {
		return new Defined<>(t);
	}

	default boolean defined() {
		return this instanceof Defined;
	}

	default void consume(Consumer<T> consumer) {
		if (this instanceof Defined<T> defined) {
			consumer.accept( defined.result() );
		}
	}

	default void consume(Consumer<T> consumer, T value) {
		if (this instanceof Defined<T> defined) {
			consumer.accept( defined.result() );
		}
		else {
			consumer.accept( value );
		}
	}

	default void consume(Consumer<T> consumer, Supplier<T> supplier) {
		if (this instanceof Defined<T> defined) {
			consumer.accept( defined.result() );
		}
		else {
			consumer.accept( supplier.get() );
		}
	}

	default T evaluate(T value) {
		return this instanceof Defined<T> defined
				? defined.result()
				: value;
	}

	default T evaluate(Supplier<T> supplier) {
		return this instanceof Defined<T> defined
				? defined.result()
				: supplier.get();
	}

	default <X> X evaluate(Function<T,X> function, Supplier<X> supplier) {
		return this instanceof Defined<T> defined
				? function.apply( defined.result() )
				: supplier.get();
	}

	default <X> X evaluate(Function<T,X> function, X value) {
		return this instanceof Defined<T> defined
				? function.apply( defined.result() )
				: value;
	}
}
