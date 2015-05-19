/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.custom;

/**
 * A return representing a {@link javax.persistence.ConstructorResult}
 *
 * @author Steve Ebersole
 */
public class ConstructorReturn implements Return {
	private final Class targetClass;
	private final ScalarReturn[] scalars;

	public ConstructorReturn(Class targetClass, ScalarReturn[] scalars) {
		this.targetClass = targetClass;
		this.scalars = scalars;
	}

	public Class getTargetClass() {
		return targetClass;
	}

	public ScalarReturn[] getScalars() {
		return scalars;
	}
}
