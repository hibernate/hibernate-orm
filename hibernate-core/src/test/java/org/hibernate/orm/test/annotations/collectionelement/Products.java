/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderBy;

@Entity
public class Products {
	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@OrderBy("name ASC")
	private Set<Widgets> widgets;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Widgets> getWidgets() {
		return widgets;
	}

	public void setWidgets(Set<Widgets> widgets) {
		this.widgets = widgets;
	}
}
