
package org.hibernate.jpa.test.criteria.fetchscroll;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "customers")
public class Customer implements Comparable<Customer>, Serializable {
	
	private Long id;
	private String name;

	public Customer() {
	}

	public Customer(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue
	@Column(name = "CUSTOMER_ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "CUSTOMER_NAME", length = 40, nullable = false, updatable = false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		return 17 * 31 + id.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		boolean result = false;
		if(object instanceof Customer) {
			Customer other = (Customer) object;
			result = id.equals(other.id);		
		}
		return result;
	}

	@Override
	public int compareTo(Customer other) {
		return name.compareTo(other.getName());
	}

}

