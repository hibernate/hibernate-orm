/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.buildtime.spi;

/**
 * Used to determine whether a class should be instrumented.
 *
 * @author Steve Ebersole
 */
public interface ClassFilter {
	/**
	 * Should this class be included in instrumentation.
	 *
	 * @param className The name of the class to check
	 *
	 * @return {@literal true} to include class in instrumentation; {@literal false} otherwise.
	 */
	public boolean shouldInstrumentClass(String className);
}
