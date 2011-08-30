//$Id$
package org.hibernate.test.annotations.manytomany;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Building {
	@Id @GeneratedValue private Long id;

	@ManyToOne @JoinColumn(name="company_id", referencedColumnName = "name")
	private BuildingCompany company;

	public BuildingCompany getCompany() {
		return company;
	}

	public void setCompany(BuildingCompany company) {
		this.company = company;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
