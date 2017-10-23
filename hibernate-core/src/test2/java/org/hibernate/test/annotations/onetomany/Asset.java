/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Asset implements Serializable {

	@Id
	@Column(name = "id_asset")
	private final Integer idAsset;

	@Id
	@Column(name = "id_test")
	private final Integer test;

	@ManyToOne(cascade = { CascadeType.ALL })
	@JoinColumn(nullable = false)
	private Employee employee;

	public Asset() {
		this.idAsset = 0;
		this.test = 1;
	}

	/**
	 * @param idAsset
	 */
	public Asset(Integer idAsset) {
		this.idAsset = idAsset;
		this.test = 1;
	}

	/**
	 * @return the id
	 */
	public Integer getIdAsset() {
		return idAsset;
	}

	/**
	 * @return the employee
	 */
	public Employee getEmployee() {
		return employee;
	}

	/**
	 * @param employee the employee to set
	 */
	public void setEmployee(Employee employee) {
		this.employee = employee;
	}
}
