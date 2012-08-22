/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.inheritance.joined;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "Parent")
public class Parent {
	private Integer id;
	private Set propertyAssets = new HashSet();
	private Set financialAssets = new HashSet();

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany(cascade = CascadeType.REFRESH,
			fetch = FetchType.EAGER,
			mappedBy = "parent",
			targetEntity = PropertyAsset.class)
	public Set getPropertyAssets() {
		return this.propertyAssets;
	}

	public void setPropertyAssets(Set propertyAssets) {
		this.propertyAssets = propertyAssets;
	}

	@OneToMany(cascade = CascadeType.REFRESH,
			fetch = FetchType.EAGER,
			mappedBy = "parent",
			targetEntity = FinancialAsset.class)
	public Set getFinancialAssets() {
		return this.financialAssets;
	}

	public void setFinancialAssets(Set financialAssets) {
		this.financialAssets = financialAssets;
	}
}
