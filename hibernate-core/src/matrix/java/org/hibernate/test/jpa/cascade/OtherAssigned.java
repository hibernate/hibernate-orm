package org.hibernate.test.jpa.cascade;


/**
 * todo: describe Other
 *
 * @author Steve Ebersole
 */
public class OtherAssigned {
	private Long id;
	private ParentAssigned owner;

	public OtherAssigned() {
	}

	public OtherAssigned(Long id) {
		this.id = id;
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
}
