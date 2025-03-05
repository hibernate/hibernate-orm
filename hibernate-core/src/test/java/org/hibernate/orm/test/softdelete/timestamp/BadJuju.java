/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.orm.test.softdelete.CustomTrueFalseConverter;

/**
 * @author Steve Ebersole
 */
@Entity
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, converter = CustomTrueFalseConverter.class)
public class BadJuju {
	@Id
	private Integer id;
	@Basic
	private String name;

	protected BadJuju() {
		// for Hibernate use
	}

	public BadJuju(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
