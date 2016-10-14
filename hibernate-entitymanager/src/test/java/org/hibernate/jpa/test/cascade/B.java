//$Id$
package org.hibernate.jpa.test.cascade;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import java.io.Serializable;

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

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof B) ) return false;

		final B b = (B) o;

		if ( !id.equals( b.id ) ) return false;

		return true;
	}

	public int hashCode() {
		return id.hashCode();
	}
}
