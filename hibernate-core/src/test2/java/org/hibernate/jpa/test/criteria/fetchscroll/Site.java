
package org.hibernate.jpa.test.criteria.fetchscroll;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "sites")
public class Site implements Serializable {

	private static final long serialVersionUID = 9213996389556805371L;

	private Long id;
	private String name;
	private Customer customer;
	
	@Id
	@GeneratedValue
	@Column(name = "SITE_ID", nullable = false, updatable = false)
	public Long getId() {	
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "SITE_NAME", length = 40, nullable = false, updatable = false)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CUSTOMER_ID", referencedColumnName = "CUSTOMER_ID", nullable = false, updatable = false)
	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	@Override
	public int hashCode() {
		return 17 * 31 + id.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		boolean result = false;
		if(object instanceof Site) {
			Site other = (Site)object;
			result = other.getId().equals(id);
		}
		return result;
	}

}
