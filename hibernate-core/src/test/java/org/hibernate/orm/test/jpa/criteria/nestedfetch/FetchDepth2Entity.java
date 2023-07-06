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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "depth3id", referencedColumnName = "id", nullable = false)
	private FetchDepth3Entity depth3Entity;

	@OneToMany(mappedBy = "depth2Entity")
	private Set<FetchDepth1Entity> depth1Entities = new HashSet<>();

	public void setId(int value) {
		this.id = value;
	}

	public int getId() {
		return id;
	}

	public void setDepth3Entity(FetchDepth3Entity value) {
		this.depth3Entity = value;
	}

	public FetchDepth3Entity getDepth3Entity() {
		return depth3Entity;
	}

	public void setDepth1Entities(Set<FetchDepth1Entity> value) {
		this.depth1Entities = value;
	}

	public Set<FetchDepth1Entity> getDepth1Entities() {
		return depth1Entities;
	}

}
