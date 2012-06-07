package org.hibernate.test.jpa.cascade;


/**
 * todo: describe ChildInfo
 *
 * @author Steve Ebersole
 */
public class ParentInfoAssigned {
	private Long id;
	private ParentAssigned owner;
	private String info;

	public ParentInfoAssigned() {
	}

	public ParentInfoAssigned(String info) {
		this.info = info;
	}

	public Long getId() {
		return id;
	}

	public ParentAssigned getOwner() {
		return owner;
	}

	public void setOwner(ParentAssigned owner) {
		this.owner = owner;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
}
