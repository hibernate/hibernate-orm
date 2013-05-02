/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.entities.onetoone;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Proxy;
import org.hibernate.envers.Audited;

/**
 * Test class for issue HHH-3854. Restricting creation of proxy objects is essential.
 */
@Entity
@Audited
@Proxy(lazy = false)
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