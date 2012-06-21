package org.hibernate.test.jpa.cascade;


/**
 * Child, but with an assigned identifier.
 *
 * @author Steve Ebersole
 */
public class ChildAssigned {
	private Long id;
	private String name;
	private ParentAssigned parent;
	private ChildInfoAssigned info;

	public ChildAssigned() {
	}

	public ChildAssigned(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ParentAssigned getParent() {
		return parent;
	}

	public void setParent(ParentAssigned parent) {
		this.parent = parent;
	}

	public ChildInfoAssigned getInfo() {
		return info;
	}

	public void setInfo(ChildInfoAssigned info) {
		this.info = info;
	}
}
