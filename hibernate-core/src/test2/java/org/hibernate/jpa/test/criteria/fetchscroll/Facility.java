
package org.hibernate.jpa.test.criteria.fetchscroll;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "facilities")
public class Facility implements Comparable<Facility>, Serializable {

	private static final long serialVersionUID = -2705232202888517103L;	

	private Long id;
	private String name;
	private Site site;
	private Customer customer;
	
	@Id
	@GeneratedValue
	@Column(name = "FACILITY_ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "FACILITY_NAME", length = 40, nullable = false, updatable = false)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SITE_ID", referencedColumnName = "SITE_ID", nullable = false, updatable = false)
	public Site getSite() {
		return site;
	}
	
	public void setSite(Site site) {
		this.site = site;
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
		if(object instanceof Facility) {
			Facility other = (Facility)object;
			result = other.getId().equals(id);
		}
		return result;
	}

	@Override
	public int compareTo(Facility other) {
		return name.compareTo(other.name);
	}

}
