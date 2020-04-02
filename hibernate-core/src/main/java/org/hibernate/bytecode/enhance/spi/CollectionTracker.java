/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi;

/**
 * Interface to be implemented by collection trackers that hold the expected size od collections, a simplified {@code Map<String, int>}.
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public interface CollectionTracker {

	void add(String name, int size);

	int getSize(String name);
}
