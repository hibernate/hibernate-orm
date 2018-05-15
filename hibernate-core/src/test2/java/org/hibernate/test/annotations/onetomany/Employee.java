/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Employee {

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "employee", orphanRemoval = true)
	@OrderBy // order by PK
	private final List<Asset> assets = new ArrayList<Asset>();

	@Id
	@Column(name = "id")
	private Integer id;

	public Employee() {

	}

	/**
	 * @param id
	 */
	public Employee(Integer id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the assets
	 */
	public List<Asset> getAssets() {
		return assets;
	}
}
