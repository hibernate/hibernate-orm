package org.hibernate.test.annotations.id.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Entity with assigned identity
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
public class Hotel {
	@Id
	private Long id;

	private String name;

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
}
