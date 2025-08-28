/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class SimpleEntityWithAssociation {
	private Long id;
	private String name;
	private Integer negatedNumber;
	private Set associatedEntities = new HashSet();
	private Set manyToManyAssociatedEntities = new HashSet();

	public SimpleEntityWithAssociation() {
	}

	public SimpleEntityWithAssociation(String name) {
		this.name = name;
	}

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

	public Integer getNegatedNumber() {
		return negatedNumber;
	}

	public void setNegatedNumber(Integer negatedNumber) {
		this.negatedNumber = negatedNumber;
	}

	public Set getAssociatedEntities() {
		return associatedEntities;
	}

	public void setAssociatedEntities(Set associatedEntities) {
		this.associatedEntities = associatedEntities;
	}

	public SimpleAssociatedEntity addAssociation(String name) {
		return new SimpleAssociatedEntity( name, this );
	}

	public void addAssociation(SimpleAssociatedEntity association) {
		association.bindToOwner( this );
	}

	public void removeAssociation(SimpleAssociatedEntity association) {
		if ( getAssociatedEntities().contains( association ) ) {
			association.unbindFromCurrentOwner();
		}
		else {
			throw new IllegalArgumentException( "SimpleAssociatedEntity [" + association + "] not currently bound to this [" + this + "]" );
		}
	}

	public Set getManyToManyAssociatedEntities() {
		return manyToManyAssociatedEntities;
	}

	public void setManyToManyAssociatedEntities(Set manyToManyAssociatedEntities) {
		this.manyToManyAssociatedEntities = manyToManyAssociatedEntities;
	}
}
