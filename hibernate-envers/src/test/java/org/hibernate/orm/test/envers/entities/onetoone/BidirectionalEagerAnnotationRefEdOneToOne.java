/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.onetoone;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Test class for issue HHH-3854. Restricting creation of proxy objects is essential.
 */
@Entity
@Audited
// Class name is too long of an identifier for Oracle.
@Table(name = "EdOneToOne")
public final class BidirectionalEagerAnnotationRefEdOneToOne {
	/**
	 * ID column.
	 */
	@Id
	@GeneratedValue
	private Integer id;

	/**
	 * Field containing the referring entity.
	 */
	@OneToOne(mappedBy = "refedOne", fetch = FetchType.EAGER)
	@NotAudited
	private BidirectionalEagerAnnotationRefIngOneToOne refIng;

	/**
	 * Field containing some data.
	 */
	private String data;

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof BidirectionalEagerAnnotationRefEdOneToOne) ) {
			return false;
		}

		BidirectionalEagerAnnotationRefEdOneToOne that = (BidirectionalEagerAnnotationRefEdOneToOne) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "BidirectionalEagerAnnotationRefEdOneToOne(id = " + id + ", data = " + data + ")";
	}

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @return the refIng
	 */
	public BidirectionalEagerAnnotationRefIngOneToOne getRefIng() {
		return refIng;
	}

	/**
	 * @param refIng the refIng to set
	 */
	public void setRefIng(BidirectionalEagerAnnotationRefIngOneToOne refIng) {
		this.refIng = refIng;
	}

	/**
	 * @return the data
	 */
	public String getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(String data) {
		this.data = data;
	}
}
