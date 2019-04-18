/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.bytecode.enhancement;

/**
 * EnhancementSelector based on class name
 *
 * @author Steve Ebersole
 */
public class ClassSelector implements EnhancementSelector {
	private final String className;

	public ClassSelector(String className) {
		this.className = className;
	}

	@Override
	public boolean select(String name) {
		return name.equals( className );
	}
}
