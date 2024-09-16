/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.attribute;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class Product extends AbstractNameable {
	private Long id;

	public Product() {
	}

	public Product(String name) {
		super( name );
	}

	@Id
	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}
}
