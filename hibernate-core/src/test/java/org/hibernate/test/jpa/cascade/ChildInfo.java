package org.hibernate.test.jpa.cascade;


/**
 * todo: describe ChildInfo
 *
 * @author Steve Ebersole
 */
public class ChildInfo {
	private Long id;
	private Child owner;
	private String info;

	public ChildInfo() {
	}

	public ChildInfo(String info) {
		this.info = info;
	}

	public Long getId() {
		return id;
	}

	public Child getOwner() {
		return owner;
	}

	public void setOwner(Child owner) {
		this.owner = owner;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
}
