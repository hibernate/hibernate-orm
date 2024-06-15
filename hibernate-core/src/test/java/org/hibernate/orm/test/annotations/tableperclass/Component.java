/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.orm.test.annotations.tableperclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity(name = "xpmComponent")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(indexes = @Index(name = "manufacturerPartNumber", columnList = "manufacturerPartNumber"))
public abstract class Component {
	private String manufacturerPartNumber;
	private Long manufacturerId;
	private Long id;

	public void setId(Long id) {
		this.id = id;
	}

	@Id
	public Long getId() {
		return id;
	}

	@Column(nullable = false)
	public String getManufacturerPartNumber() {
		return manufacturerPartNumber;
	}

	@Column(nullable = false)
	public Long getManufacturerId() {
		return manufacturerId;
	}

	public void setManufacturerId(Long manufacturerId) {
		this.manufacturerId = manufacturerId;
	}


	public void setManufacturerPartNumber(String manufacturerPartNumber) {
		this.manufacturerPartNumber = manufacturerPartNumber;
	}
}
