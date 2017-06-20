
package org.hibernate.jpa.test.criteria.fetchscroll;

import java.util.List;
import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "purchase_organizations")
public class PurchaseOrg implements Serializable {

	private static final long serialVersionUID = -6659835148502079000L;

	private Long id;
	private String name;	
	private Customer customer;
	private List<Facility> facilities;
	
	@Id
	@GeneratedValue
	@Column(name = "PURCHASE_ORG_ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "PURCHASE_ORG_NAME", length = 40, nullable = false, updatable = false)
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
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "PURCHASE_FACILITY",
		joinColumns = @JoinColumn(name = "PURCHASE_ORG_ID"),
		inverseJoinColumns = @JoinColumn(name = "FACILITY_ID"))
	public List<Facility> getFacilities() {	
		return facilities;
	}
	
	public void setFacilities(List<Facility> facilities) {
		this.facilities = facilities;
	}
		
	@Override
	public int hashCode() {
		return 17 * 31 + id.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		boolean result = false;
		if(object instanceof PurchaseOrg) {
			PurchaseOrg other = (PurchaseOrg) object;
			result = id.equals(other.id);
		}
		return result;
	}
	
}
