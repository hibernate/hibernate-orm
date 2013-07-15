package org.hibernate.test.hqlfetchscroll;

public class Child {

	// A numeric id must be the <id> field.  Some databases (Sybase, etc.)
	// require identifier columns in order to support scrollable results.
	private long id;
	private String name;

	Child() {
	}

	public Child(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}
}
