//$Id$
package org.hibernate.test.annotations.manytomany;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Building {
	@Column(unique = true)
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
