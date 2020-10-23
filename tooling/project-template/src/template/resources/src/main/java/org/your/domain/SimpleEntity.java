package org.your.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * A simple example entity
 */
@Entity
public class SimpleEntity {
	@Id
	private Integer id;
	private String name;

	private SimpleEntity() {
	}

	public SimpleEntity(Integer id) {
		this.id = id;
	}

	public SimpleEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
