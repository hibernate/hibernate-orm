/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.nullable;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.NaturalId;

/**
 * @author Guenther Demetz
 */
@Entity
public class A  {
	@Id
	public int oid;

	@ManyToOne
	@NaturalId(mutable=true)
	public C assC;

	@Column
	@NaturalId(mutable=true)
	public String myname;

	@OneToMany(mappedBy="assA")
	public Set<B> assB = new HashSet<B>();

	public A() {
	}

	public A(int oid, C assC, String myname) {
		this.oid = oid;
		this.assC = assC;
		this.myname = myname;
	}
}
