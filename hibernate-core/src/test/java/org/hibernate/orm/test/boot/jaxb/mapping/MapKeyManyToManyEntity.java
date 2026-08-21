/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import java.util.HashMap;
import java.util.Map;

public class MapKeyManyToManyEntity {
	private Long id;
	private Map<SimpleEntity, MapKeyManyToManyEntity> managers = new HashMap<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Map<SimpleEntity, MapKeyManyToManyEntity> getManagers() {
		return managers;
	}

	public void setManagers(Map<SimpleEntity, MapKeyManyToManyEntity> managers) {
		this.managers = managers;
	}
}
