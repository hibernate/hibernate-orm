package org.hibernate.orm.test.jpa.criteria.nestedfetch;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class FetchRootEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column (name = "val")
	private String value;

	public FetchRootEntity() {
	}

	public FetchRootEntity(String value) {
		this.value = value;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@OneToMany(mappedBy = "rootEntity")
	private Set<FetchDepth1Entity> depth1Entities = new HashSet<>();

	public Set<FetchDepth1Entity> getDepth1Entities() {
		return depth1Entities;
	}

	public void setDepth1Entities(Set<FetchDepth1Entity> depth1Entities) {
		this.depth1Entities = depth1Entities;
	}
}
