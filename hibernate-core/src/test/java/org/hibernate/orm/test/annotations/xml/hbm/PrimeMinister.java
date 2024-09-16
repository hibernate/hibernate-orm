/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.hbm;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class PrimeMinister {
	private Integer id;
	private String name;
	private Government currentGovernment;
	private Set<Government> governments;

	@ManyToOne
	public Government getCurrentGovernment() {
		return currentGovernment;
	}

	public void setCurrentGovernment(Government currentGovernment) {
		this.currentGovernment = currentGovernment;
	}

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

	@OneToMany(mappedBy = "primeMinister")
	public Set<Government> getGovernments() {
		return governments;
	}

	public void setGovernments(Set<Government> governments) {
		this.governments = governments;
	}

}
