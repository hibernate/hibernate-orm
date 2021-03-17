/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.bytecode.enhancement;

/**
 * @author Steve Ebersole
 */
public class EverythingSelector implements EnhancementSelector {
	/**
	 * Singleton access
	 */
	public static final EverythingSelector INSTANCE = new EverythingSelector();

	@Override
	public boolean select(String name) {
		return true;
	}
}
