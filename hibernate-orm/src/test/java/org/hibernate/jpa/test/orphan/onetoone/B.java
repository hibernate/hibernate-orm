/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.orphan.onetoone;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * @author Martin Simka
 */
@Entity
public class B implements Serializable {
	private Integer id;
	private A a;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToOne(targetEntity=A.class, cascade=CascadeType.ALL,optional = true, orphanRemoval = true)
	@JoinColumn(name="FK_FOR_B")
	public A getA() {
		return a;
	}

	public void setA(A a) {
		this.a = a;
	}
}
