/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade.multilevel;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "BOTTOM")
public class Bottom {
	@Id
	@GeneratedValue
	private Long id;
	@OneToOne(mappedBy = "bottom")
	private Middle middle;

	Long getId() {
		return id;
	}

	void setId(Long id) {
		this.id = id;
	}

	Middle getMiddle() {
		return middle;
	}

	void setMiddle(Middle middle) {
		this.middle = middle;
	}
}
