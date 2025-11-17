/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.embeddedid;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * @author Justin Wesley
 * @author Steve Ebersole
 */
@Embeddable
public class ProductId implements Serializable {
	private Integer id;
	private String code;

	public ProductId() {
	}

	@Column
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
