/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "Parent")
public class Parent {
	private Integer id;
	private Set propertyAssets = new HashSet();
	private Set financialAssets = new HashSet();

	@Id @GeneratedValue public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER, mappedBy = "parent", targetEntity = PropertyAsset.class)
	public Set getPropertyAssets() {
		return this.propertyAssets;
	}

	public void setPropertyAssets(Set propertyAssets) {
		this.propertyAssets = propertyAssets;
	}

	@OneToMany(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER, mappedBy = "parent", targetEntity = FinancialAsset.class)
	public Set getFinancialAssets() {
		return this.financialAssets;
	}

	public void setFinancialAssets(Set financialAssets) {
		this.financialAssets = financialAssets;
	}
}
