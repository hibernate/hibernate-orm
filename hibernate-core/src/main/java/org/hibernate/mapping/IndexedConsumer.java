/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

/**
 * A consumer like {@link java.util.function.Consumer} but also accepts an integer as index.
 *
 * @author Christian Beikov
 */
@FunctionalInterface
public interface IndexedConsumer<T> {

	void accept(int index, T t);
}
