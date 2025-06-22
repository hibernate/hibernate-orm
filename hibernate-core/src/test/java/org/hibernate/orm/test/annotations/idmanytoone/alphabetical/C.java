/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone.alphabetical;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;

@Entity
@IdClass( CId.class )
public class C {

	@Id
	private A parent;

	@Id
	private int sequenceNumber;

	@OneToMany( mappedBy = "parent" )
	List<B> children;

	public C() {
	}

	public A getParent() {
		return parent;
	}

	public void setParent(A parent) {
		this.parent = parent;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public List<B> getChildren() {
		return children;
	}

	public void setChildren(List<B> children) {
		this.children = children;
	}


}
