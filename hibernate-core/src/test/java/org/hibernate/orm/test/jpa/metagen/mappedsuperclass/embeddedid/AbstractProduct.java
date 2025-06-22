/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.embeddedid;

import java.io.Serializable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Justin Wesley
 * @author Steve Ebersole
 */
@MappedSuperclass
public class AbstractProduct implements Serializable {
	private ProductId id;

	public AbstractProduct() {
	}

	@EmbeddedId
	public ProductId getId() {
		return id;
	}

	public void setId(ProductId id) {
		this.id = id;
	}
}
