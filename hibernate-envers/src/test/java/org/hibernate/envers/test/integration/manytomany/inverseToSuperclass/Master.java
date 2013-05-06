package org.hibernate.envers.test.integration.manytomany.inverseToSuperclass;

import java.util.List;

import org.hibernate.envers.Audited;

@Audited
public class Master {

	private long id;

	private String str;

	private List<DetailSubclass> items;

	public Master() {

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public List<DetailSubclass> getItems() {
		return items;
	}

	public void setItems(List<DetailSubclass> items) {
		this.items = items;
	}

}
