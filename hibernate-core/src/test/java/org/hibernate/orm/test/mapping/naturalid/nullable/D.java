/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.nullable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;

/**
 * @author Guenther Demetz
 */
@Entity
public class D {
	@Id
	public int oid;

	@NaturalId(mutable=true)
	public String name;

	@NaturalId(mutable=true)
	@ManyToOne
	public C associatedC;

	public D() {
	}

	public D(int oid, String name, C associatedC) {
		this.oid = oid;
		this.name = name;
		this.associatedC = associatedC;
	}
}
