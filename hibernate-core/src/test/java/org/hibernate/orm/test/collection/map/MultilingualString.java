/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.map;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "multilingual")
public class MultilingualString {
	@Id
	@GeneratedValue
	private long id;

	@ElementCollection
	@MapKeyColumn(name = "language", insertable = false, updatable = false)
	@CollectionTable(name = "multilingual_string_map", joinColumns = @JoinColumn(name = "string_id"))
	private Map<String, LocalizedString> map = new HashMap<String, LocalizedString>();

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public Map<String, LocalizedString> getMap() {
		return map;
	}
	public void setMap(Map<String, LocalizedString> map) {
		this.map = map;
	}
}
