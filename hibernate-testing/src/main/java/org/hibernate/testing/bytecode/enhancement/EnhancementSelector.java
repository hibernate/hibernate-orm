/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.bytecode.enhancement;

/**
 * Used by {@link BytecodeEnhancerRunner} to determine which classes should
 * be enhanced.
 *
 * @author Steve Ebersole
 */
public interface EnhancementSelector {
	/**
	 * Determine whether the named class should be enhanced.
	 */
	boolean select(String name);
}
