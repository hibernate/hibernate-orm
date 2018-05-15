/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2.joincolumn;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class Location {
	private Integer id;
	private String code;
	private Company company;

	public Location() {
	}

	public Location(Integer id, String code, Company company) {
		this.id = id;
		this.code = code;
		this.company = company;

		company.addLocation( this );
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@ManyToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(name = "comp_id", referencedColumnName = "id", nullable = false)
	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}
}
