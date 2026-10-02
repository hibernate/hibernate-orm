package org.hibernate.orm.test.ondelete.toone.hbm;

/**
 * @author Vlad Mihalcea
 */
public class Child {

	private Long id;

	private Parent parent;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
