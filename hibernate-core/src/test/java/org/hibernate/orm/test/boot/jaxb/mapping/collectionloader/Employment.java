package org.hibernate.orm.test.boot.jaxb.mapping.collectionloader;

public class Employment {
	private Long id;
	private Owner owner;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Owner getOwner() {
		return owner;
	}

	public void setOwner(Owner owner) {
		this.owner = owner;
	}
}
