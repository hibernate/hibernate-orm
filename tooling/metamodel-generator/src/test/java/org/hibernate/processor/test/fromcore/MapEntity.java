/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.fromcore;

import jakarta.persistence.*;

import java.util.Map;

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
