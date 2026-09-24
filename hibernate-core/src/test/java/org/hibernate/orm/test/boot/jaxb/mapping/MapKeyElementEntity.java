/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import java.util.HashMap;
import java.util.Map;

public class MapKeyElementEntity {
	private Long id;
	private Map<SimpleEntity, String> textItem = new HashMap<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Map<SimpleEntity, String> getTextItem() {
		return textItem;
	}

	public void setTextItem(Map<SimpleEntity, String> textItem) {
		this.textItem = textItem;
	}
}
