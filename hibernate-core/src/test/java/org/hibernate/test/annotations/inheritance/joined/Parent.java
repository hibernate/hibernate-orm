/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.inheritance.joined;
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
