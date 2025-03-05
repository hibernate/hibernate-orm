/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.collection;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Child {

	private Integer id;
	private Parent daddy;

	public Child() {

	}

	@Id
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne
	public Parent getDaddy() {
		return daddy;
	}
	public void setDaddy(Parent daddy) {
		this.daddy = daddy;
	}


}
