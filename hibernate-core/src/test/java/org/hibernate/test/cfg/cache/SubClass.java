package org.hibernate.test.cfg.cache;

import java.util.HashSet;
import java.util.Set;

public class SubClass extends BaseClass {
	Set<Item> items = new HashSet<Item>();

	public Set<Item> getItems() {
		return items;
	}

	public void setItems(Set<Item> items) {
		this.items = items;
	}

}
