//$Id$
package org.hibernate.jpa.test;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Distributor implements Serializable {
	private Integer id;
	private String name;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Distributor ) ) return false;

		final Distributor distributor = (Distributor) o;

		if ( !name.equals( distributor.name ) ) return false;

		return true;
	}

	public int hashCode() {
		return name.hashCode();
	}
}
