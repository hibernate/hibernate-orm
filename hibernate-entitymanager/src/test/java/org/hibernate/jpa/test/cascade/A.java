//$Id$
package org.hibernate.jpa.test.cascade;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.io.Serializable;

/**
 * @author Martin Simka
 */
@Entity
public class A implements Serializable {
	private Integer id;
	private B b;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToOne(targetEntity=B.class, mappedBy="a", orphanRemoval = true)
	public B getB() {
		return b;
	}

	public void setB(B b) {
		this.b = b;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof A) ) return false;

		final A a = (A) o;

		if ( !id.equals( a.id ) ) return false;

		return true;
	}

	public int hashCode() {
		return id.hashCode();
	}
}
