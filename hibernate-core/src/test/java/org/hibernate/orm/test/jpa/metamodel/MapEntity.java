/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

@Entity
@Table( name = "MAP_ENTITY" )
public class MapEntity {

	@Id
	@Column(name="key_")
	private String key;

	@ElementCollection(fetch=FetchType.LAZY)
	@CollectionTable(name="MAP_ENTITY_NAME", joinColumns=@JoinColumn(name="key_"))
	@MapKeyColumn(name="lang_")
	private Map<String, MapEntityLocal> localized;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
