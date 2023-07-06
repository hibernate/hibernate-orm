package org.hibernate.orm.test.jpa.criteria.nestedfetch;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class FetchDepth1PK implements Serializable {
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="rootId", referencedColumnName="id", nullable=false)
	private FetchRootEntity rootEntity;
	
	public void setRootEntity(FetchRootEntity value)  {
		this.rootEntity =  value;
	}
	
	public FetchRootEntity getRootEntity()  {
		return this.rootEntity;
	}
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="depth2Id", referencedColumnName="id", nullable=false)
	private FetchDepth2Entity depth2Entity;
	
	public void setDepth2Entity(FetchDepth2Entity value)  {
		this.depth2Entity =  value;
	}
	
	public FetchDepth2Entity getDepth1Entity()  {
		return this.depth2Entity;
	}
	
}
