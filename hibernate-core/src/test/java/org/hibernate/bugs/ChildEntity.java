package org.hibernate.bugs;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
class ChildEntity {
	private Long id;

	private String name;

	private SimpleEntity parent;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn
	@LazyToOne(LazyToOneOption.PROXY)
	public SimpleEntity getParent() {
		return parent;
	}

	public void setParent(final SimpleEntity parent) {
		this.parent = parent;
	}

}