package org.hibernate.test.component.empty;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class ComponentEmptyEmbeddedOwner {

	@Id
	@GeneratedValue
	private Integer id;

	private ComponentEmptyEmbedded embedded;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ComponentEmptyEmbedded getEmbedded() {
		return embedded;
	}

	public void setEmbedded(ComponentEmptyEmbedded embedded) {
		this.embedded = embedded;
	}

}
