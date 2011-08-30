//$Id$
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
