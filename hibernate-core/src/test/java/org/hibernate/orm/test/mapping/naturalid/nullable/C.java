/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.nullable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.NaturalId;

/**
 * @author Guenther Demetz
 */
@Entity
public class C {
	@Id
	public int oid;

	@NaturalId(mutable=true)
	public String name;

	public C() {
	}

	public C(int oid, String name) {
		this.oid = oid;
		this.name = name;
	}
}
