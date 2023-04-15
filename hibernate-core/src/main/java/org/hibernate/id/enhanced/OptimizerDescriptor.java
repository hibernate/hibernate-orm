/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

/**
 * @author Gavin King
 */
public interface OptimizerDescriptor {
	boolean isPooled();
	String getExternalName();
	Class<? extends Optimizer> getOptimizerClass()
			throws ClassNotFoundException;
}
