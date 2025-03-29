/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.timestamp;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import org.hibernate.annotations.Bag;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.orm.test.softdelete.CustomTrueFalseConverter;

import java.util.List;

/**
 * @author Steve Ebersole
 */
@Entity
public class BadAss {
	@Id
	private Integer id;
	@Basic
	private String name;
	@Bag
	@ElementCollection
	@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, converter = CustomTrueFalseConverter.class)
	private List<String> labels;

	protected BadAss() {
		// for Hibernate use
	}

	public BadAss(Integer id, String name) {
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
