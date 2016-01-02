
package org.hibernate.jpa.test.criteria.fetchscroll;

import javax.persistence.*;

@Entity
@Table(name = "products")
public class Product {

	private ProductId id;	
	private String number;
	private Facility facility;

	public Product() {
	}

	public Product(Facility facility, String number) {
		this.id = new ProductId();
		this.id.setFacilityId(facility.getId());
		this.id.setItemId(1L);
		this.number = number;
		this.facility = facility;
	}
	
	@Id
	private ProductId getId() {
		return id;
	}
	
	public void setId(ProductId id) {
		this.id = id;
	}
	
	@Column(name = "PRODUCT_NUMBER", length = 40, nullable = false, updatable = false)
	public String getNumber() {
		return number;
	}
	
	public void setNumber(String number) {
		this.number = number;
	} 
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "FACILITY_ID", referencedColumnName = "FACILITY_ID", nullable = false, insertable = false, updatable = false)
	public Facility getFacility() {
		return facility;
	}
	
	public void setFacility(Facility facility) {
		this.facility = facility;
	}
	
}
