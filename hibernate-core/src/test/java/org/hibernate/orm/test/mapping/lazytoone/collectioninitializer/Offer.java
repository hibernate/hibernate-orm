package org.hibernate.orm.test.mapping.lazytoone.collectioninitializer;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import static javax.persistence.FetchType.LAZY;

@Entity
public class Offer {
	@Id
	private Long id;

	@ManyToOne(fetch = LAZY, optional = false)
	private CostCenter costCenter;

	@Override
	public String toString() {
		return "Offer{" +
				"id=" + getId() +
				'}';
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CostCenter getCostCenter() {
		return costCenter;
	}

	public void setCostCenter(CostCenter costCenter) {
		this.costCenter = costCenter;
	}
}
