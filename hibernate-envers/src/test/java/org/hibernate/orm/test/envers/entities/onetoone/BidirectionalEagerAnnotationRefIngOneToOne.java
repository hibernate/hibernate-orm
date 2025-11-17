/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.onetoone;

import org.hibernate.envers.Audited;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Test class for issue HHH-3854. Restricting creation of proxy objects is essential.
 */
@Entity
@Audited
//Class name is too long of an identifier for Oracle.
@Table(name = "IngOneToOne")
public final class BidirectionalEagerAnnotationRefIngOneToOne {
	/**
	 * ID column.
	 */
	@Id
	@GeneratedValue
	private Integer id;

	/**
	 * Field with an optional first referred entity.
	 */
	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, optional = true)
	@JoinColumn(name = "REF_ID_ONE")
	private BidirectionalEagerAnnotationRefEdOneToOne refedOne = null;

	/**
	 * Field with an optional second referred entity.
	 */
	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, optional = true)
	@JoinColumn(name = "REF_ID_TWO")
	private BidirectionalEagerAnnotationRefEdOneToOne refedTwo = null;

	/**
	 * Field containing some data.
	 */
	private String data;

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof BidirectionalEagerAnnotationRefIngOneToOne) ) {
			return false;
		}

		BidirectionalEagerAnnotationRefIngOneToOne that = (BidirectionalEagerAnnotationRefIngOneToOne) o;

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
		return "BidirectionalEagerAnnotationRefIngOneToOne(id = " + id + ", data = " + data + ")";
	}

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @return the refedOne
	 */
	public BidirectionalEagerAnnotationRefEdOneToOne getRefedOne() {
		return refedOne;
	}

	/**
	 * @param refedOne the refedOne to set
	 */
	public void setRefedOne(BidirectionalEagerAnnotationRefEdOneToOne refedOne) {
		this.refedOne = refedOne;
	}

	/**
	 * @return the refedTwo
	 */
	public BidirectionalEagerAnnotationRefEdOneToOne getRefedTwo() {
		return refedTwo;
	}

	/**
	 * @param refedTwo the refedTwo to set
	 */
	public void setRefedTwo(BidirectionalEagerAnnotationRefEdOneToOne refedTwo) {
		this.refedTwo = refedTwo;
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
