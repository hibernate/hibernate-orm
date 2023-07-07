package org.hibernate.orm.test.jpa.criteria.nestedfetch;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

@Entity
@IdClass(FetchDepth1PK.class)
public class FetchDepth1Entity implements Serializable {

	public FetchDepth1Entity() {
		//empty
	}

	@Id
	@PrimaryKeyJoinColumn (name = "rootId", referencedColumnName = "id")
	@ManyToOne(fetch = FetchType.LAZY)
	private FetchRootEntity rootEntity;

	@Id
	@PrimaryKeyJoinColumn (name = "depth2Id", referencedColumnName = "id")
	@ManyToOne(fetch = FetchType.LAZY)
	private FetchDepth2Entity depth2Entity;

	public void setRootEntity(FetchRootEntity value) {
		this.rootEntity = value;
	}

	public FetchRootEntity getRootEntity() {
		return rootEntity;
	}

	public void setDepth2Entity(FetchDepth2Entity value) {
		this.depth2Entity = value;
	}

	public FetchDepth2Entity getDepth2Entity() {
		return depth2Entity;
	}

}
