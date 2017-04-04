/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.serialization.entity;

/**
 * The class should be in a package that is different from the test
 * so that the test does not have access to private field,
 * and the protected getter and setter.
 *
 * @author Gail Badner
 */
public class AnEntity {
	private PK pk;

	public AnEntity() {
	}

	public AnEntity(PK pk) {
		this.pk = pk;
	}

	protected PK getPk() {
		return pk;
	}

	protected void setPk(PK pk) {
		this.pk = pk;
	}
}
