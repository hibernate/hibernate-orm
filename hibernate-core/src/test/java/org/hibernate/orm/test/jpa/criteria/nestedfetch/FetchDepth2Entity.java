package org.hibernate.orm.test.jpa.criteria.nestedfetch;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class FetchDepth2Entity implements Serializable {

	public FetchDepth2Entity() {
		//leer
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@OneToMany(mappedBy = "depth2Entity")
	private Set<FetchDepth1Entity> depth1Entities = new HashSet<>();

	private String val;

	public void setId(int value) {
		this.id = value;
	}

	public int getId() {
		return id;
	}

	public void setDepth1Entities(Set<FetchDepth1Entity> value) {
		this.depth1Entities = value;
	}

	public Set<FetchDepth1Entity> getDepth1Entities() {
		return depth1Entities;
	}

	public String getVal() {
		return val;
	}

	public void setVal(String val) {
		this.val = val;
	}
}
