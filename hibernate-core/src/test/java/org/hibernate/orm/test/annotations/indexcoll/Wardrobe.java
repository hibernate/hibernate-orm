/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;

import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.annotations.ListIndexBase;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Wardrobe {

	private Long id;
	private List<Drawer> drawers;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * unidirectional one to many list  with non null foreign key (mapping
	 * not recommended).
	 */
	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "wardrobe_id", nullable = false)
	@OrderColumn( name = "drawer_position" )
	@ListIndexBase( 1 )
	public List<Drawer> getDrawers() {
		return drawers;
	}

	public void setDrawers(List<Drawer> drawers) {
		this.drawers = drawers;
	}
}
