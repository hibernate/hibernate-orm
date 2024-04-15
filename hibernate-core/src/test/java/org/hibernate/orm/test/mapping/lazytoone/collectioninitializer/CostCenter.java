package org.hibernate.orm.test.mapping.lazytoone.collectioninitializer;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class CostCenter {
	@Id
	private Long id;

	@ManyToOne(optional = false)
	private Company company;

	@Override
	public String toString() {
		return "CostCenter{" +
				"id=" + id +
				", company=" + company +
				'}';
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

}
