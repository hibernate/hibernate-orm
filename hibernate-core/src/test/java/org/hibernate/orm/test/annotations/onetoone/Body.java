/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Body {
	private Integer id;
	private Heart heart;

	@OneToOne
	@PrimaryKeyJoinColumn
	public Heart getHeart() {
		return heart;
	}

	public void setHeart(Heart heart) {
		this.heart = heart;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
