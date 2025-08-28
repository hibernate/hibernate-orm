/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.map;

import jakarta.persistence.*;

@Entity
@Table(name = "multilingual_parent")
public class MultilingualStringParent {

	@Id
	@GeneratedValue
	private long id;
	@ManyToOne(fetch = FetchType.LAZY)
	private MultilingualStringParent parent;
	@ManyToOne(fetch = FetchType.LAZY)
	private MultilingualString string;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public MultilingualStringParent getParent() {
		return parent;
	}

	public void setParent(MultilingualStringParent parent) {
		this.parent = parent;
	}

	public MultilingualString getString() {
		return string;
	}

	public void setString(MultilingualString string) {
		this.string = string;
	}
}
