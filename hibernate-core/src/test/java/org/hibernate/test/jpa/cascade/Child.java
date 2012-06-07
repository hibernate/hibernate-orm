package org.hibernate.test.jpa.cascade;


/**
 * todo: describe Child
 *
 * @author Steve Ebersole
 */
public class Child {
	private Long id;
	private String name;
	private Parent parent;
	private ChildInfo info;

	public Child() {
	}

	public Child(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}

	public ChildInfo getInfo() {
		return info;
	}

	public void setInfo(ChildInfo info) {
		this.info = info;
	}
}
