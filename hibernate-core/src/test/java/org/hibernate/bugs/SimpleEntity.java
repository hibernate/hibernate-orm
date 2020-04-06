package org.hibernate.bugs;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
class SimpleEntity {

	private Long id;

	private String name;

	Set<ChildEntity> children = new HashSet<>();

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

	@OneToMany(targetEntity = ChildEntity.class, mappedBy = "parent")
	// @OneToMany is fetch = FetchType.LAZY by default
	@LazyCollection(LazyCollectionOption.EXTRA)
	@Fetch(FetchMode.SELECT)
	public Set<ChildEntity> getChildren() {
		return children;
	}

	public void setChildren(final Set<ChildEntity> children) {
		this.children = children;
	}

}