package org.hibernate.test.jpa.cascade;


/**
 * todo: describe ChildInfo
 *
 * @author Steve Ebersole
 */
public class ParentInfo {
	private Long id;
	private Parent owner;
	private String info;

	public ParentInfo() {
	}

	public ParentInfo(String info) {
		this.info = info;
	}

	public Long getId() {
		return id;
	}

	public Parent getOwner() {
		return owner;
	}

	public void setOwner(Parent owner) {
		this.owner = owner;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
}
