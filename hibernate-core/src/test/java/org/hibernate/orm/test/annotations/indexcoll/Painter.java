/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Painter {
	private Integer id;
	private Map<String, Painting> paintings = new HashMap<String, Painting>();

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany(cascade = {CascadeType.ALL})
	@MapKey(name = "name")
	@JoinColumn
	public Map<String, Painting> getPaintings() {
		return paintings;
	}

	public void setPaintings(Map<String, Painting> paintings) {
		this.paintings = paintings;
	}
}
