/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class BuildingCompany extends Company {
	@Id @GeneratedValue private Long id;
	private Date foundedIn;

	public Date getFoundedIn() {
		return foundedIn;
	}

	public void setFoundedIn(Date foundedIn) {
		this.foundedIn = foundedIn;
	}

	public Long getId() {
		return id;
	}

}
