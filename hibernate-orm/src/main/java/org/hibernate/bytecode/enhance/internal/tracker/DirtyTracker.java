/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.tracker;

/**
 * Interface to be implemented by dirty trackers, a simplified Set of String.
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public interface DirtyTracker {

	void add(String name);

	boolean contains(String name);

	void clear();

	boolean isEmpty();

	String[] get();

	void suspend(boolean suspend);
}
