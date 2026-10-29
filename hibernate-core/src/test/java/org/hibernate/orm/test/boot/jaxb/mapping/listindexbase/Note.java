package org.hibernate.orm.test.boot.jaxb.mapping.listindexbase;

import java.util.List;

public class Note {
	private Long id;
	private String altKey;
	private List items;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAltKey() {
		return altKey;
	}

	public void setAltKey(String altKey) {
		this.altKey = altKey;
	}

	public List getItems() {
		return items;
	}

	public void setItems(List items) {
		this.items = items;
	}
}
