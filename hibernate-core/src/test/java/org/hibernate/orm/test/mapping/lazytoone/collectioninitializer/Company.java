package org.hibernate.orm.test.mapping.lazytoone.collectioninitializer;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Company {
	@Id
	private Long id;

	@Override
	public String toString() {
		return "Company{" +
				"id=" + id +
				'}';
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
