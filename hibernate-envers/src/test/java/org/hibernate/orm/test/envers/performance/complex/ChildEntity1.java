/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.performance.complex;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class ChildEntity1 {
	@Id
	private Long id;

	@ManyToOne(cascade = CascadeType.ALL)
	private ChildEntity2 child1;

	@ManyToOne(cascade = CascadeType.ALL)
	private ChildEntity2 child2;

	private String data1;

	private String data2;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ChildEntity2 getChild1() {
		return child1;
	}

	public void setChild1(ChildEntity2 child1) {
		this.child1 = child1;
	}

	public ChildEntity2 getChild2() {
		return child2;
	}

	public void setChild2(ChildEntity2 child2) {
		this.child2 = child2;
	}

	public String getData1() {
		return data1;
	}

	public void setData1(String data1) {
		this.data1 = data1;
	}

	public String getData2() {
		return data2;
	}

	public void setData2(String data2) {
		this.data2 = data2;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ChildEntity1) ) {
			return false;
		}

		ChildEntity1 that = (ChildEntity1) o;

		if ( data1 != null ? !data1.equals( that.data1 ) : that.data1 != null ) {
			return false;
		}
		if ( data2 != null ? !data2.equals( that.data2 ) : that.data2 != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (data1 != null ? data1.hashCode() : 0);
		result = 31 * result + (data2 != null ? data2.hashCode() : 0);
		return result;
	}
}
