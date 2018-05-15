
package org.hibernate.jpa.test.criteria.fetchscroll;

import java.io.Serializable;
import javax.persistence.*;

@Embeddable
public class ProductId implements Serializable {

	private Long facilityId;
	private Long itemId;

	@Column(name = "FACILITY_ID", nullable = false, updatable = false)
	public Long getFacilityId() {
		return facilityId;
	}

	public void setFacilityId(Long facilityId) {
		this.facilityId = facilityId;	
	}

	@Column(name = "PRODUCT_ID", nullable = false, updatable = false)
	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

}

