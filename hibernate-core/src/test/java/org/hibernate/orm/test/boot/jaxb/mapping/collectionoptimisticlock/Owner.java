/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.collectionoptimisticlock;

import java.util.List;

public class Owner {
	private Long id;
	private List lockedItems;
	private List unlockedItems;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List getLockedItems() {
		return lockedItems;
	}

	public void setLockedItems(List lockedItems) {
		this.lockedItems = lockedItems;
	}

	public List getUnlockedItems() {
		return unlockedItems;
	}

	public void setUnlockedItems(List unlockedItems) {
		this.unlockedItems = unlockedItems;
	}
}
