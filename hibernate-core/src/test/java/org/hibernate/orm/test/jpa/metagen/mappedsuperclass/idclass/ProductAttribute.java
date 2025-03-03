/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.idclass;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass(value = ProductAttributeId.class)
public class ProductAttribute extends AbstractAttribute implements Serializable {
	private String owner;

	public ProductAttribute(String key, String value, String product) {
		this.key = key;
		this.value = value;
		this.owner = product;
	}

	public ProductAttribute() {
		super();
	}

	@Id
	@Column(name = "owner")
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Id
	@Column(name = "attribute_key")
	public String getKey() {
		return key;
	}

}
