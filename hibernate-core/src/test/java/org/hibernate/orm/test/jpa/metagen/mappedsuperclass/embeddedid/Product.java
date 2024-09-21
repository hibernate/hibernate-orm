/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.embeddedid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * @author Justin Wesley
 * @author Steve Ebersole
 */
@Entity
public class Product extends AbstractProduct {

	private String description;

	public Product() {
	}

	@Column
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
