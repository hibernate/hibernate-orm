package org.hibernate.test.jpa.cascade;


/**
 * todo: describe Other
 *
 * @author Steve Ebersole
 */
public class Other {
	private Long id;
	private Parent owner;

	public Long getId() {
		return id;
	}

	public Parent getOwner() {
		return owner;
	}

	public void setOwner(Parent owner) {
		this.owner = owner;
	}
}
