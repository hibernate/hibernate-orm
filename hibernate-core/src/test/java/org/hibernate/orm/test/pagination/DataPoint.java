/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.DynamicUpdate;

/**
 * @author Gavin King
 */
@Entity
@DynamicUpdate
public class DataPoint {
	@Id
	@GeneratedValue
	private long id;
	@Column(name = "seqval", nullable = false)
	private int sequence;
	@Column(name = "xval", nullable = false, precision = 20, scale = 19)
	private BigDecimal x;
	@Column(name = "yval", nullable = false, precision = 20, scale = 19)
	private BigDecimal y;
	private String description;

	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Getter for property 'sequence'.
	 *
	 * @return Value for property 'sequence'.
	 */
	public int getSequence() {
		return sequence;
	}

	/**
	 * Setter for property 'sequence'.
	 *
	 * @param sequence Value to set for property 'sequence'.
	 */
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return Returns the x.
	 */
	public BigDecimal getX() {
		return x;
	}

	/**
	 * @param x The x to set.
	 */
	public void setX(BigDecimal x) {
		this.x = x;
	}

	/**
	 * @return Returns the y.
	 */
	public BigDecimal getY() {
		return y;
	}

	/**
	 * @param y The y to set.
	 */
	public void setY(BigDecimal y) {
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		DataPoint dataPoint = (DataPoint) o;

		if ( sequence != dataPoint.sequence ) {
			return false;
		}
		if ( description != null ? !description.equals( dataPoint.description ) : dataPoint.description != null ) {
			return false;
		}
		if ( x != null ? !x.equals( dataPoint.x ) : dataPoint.x != null ) {
			return false;
		}
		if ( y != null ? !y.equals( dataPoint.y ) : dataPoint.y != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = sequence;
		result = 31 * result + ( x != null ? x.hashCode() : 0 );
		result = 31 * result + ( y != null ? y.hashCode() : 0 );
		result = 31 * result + ( description != null ? description.hashCode() : 0 );
		return result;
	}
}
