package org.hibernate.test.component.cascading.toone;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Document {
	private Long id;
	private String location;
	private User owner;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}
}
