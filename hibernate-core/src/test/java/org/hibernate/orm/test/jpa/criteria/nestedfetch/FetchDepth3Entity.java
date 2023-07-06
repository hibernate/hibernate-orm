package org.hibernate.orm.test.jpa.criteria.nestedfetch;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class FetchDepth3Entity implements Serializable {

	public FetchDepth3Entity() {
		//empty
	}

	public FetchDepth3Entity(String value) {
		this.value = value;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(name = "val")
	private String value;

	@OneToMany(mappedBy = "depth3Entity")
	private Set<FetchDepth2Entity> depth2Entities = new HashSet<>();

	public void setId(int value) {
		this.id = value;
	}

	public int getId() {
		return id;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setDepth2Entities(Set<FetchDepth2Entity> value) {
		this.depth2Entities = value;
	}

	public Set<FetchDepth2Entity> getDepth2Entities() {
		return depth2Entities;
	}
}
