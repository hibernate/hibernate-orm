/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.serialization.entity;

/**
 * This class should be in a package that is different from the test
 * so that the test and entity that uses this class for its primary
 * key does not have access to private field, and the protected getter and setter.
 *
 * @author Gail Badner
 */
public class PK {
	private Long value;

	public PK() {
	}

	public PK(Long value) {
		this.value = value;
	}

	protected Long getValue() {
		return value;
	}

	protected void setValue(Long value) {
		this.value = value;
	}
}
