/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.pagination;
import java.math.BigDecimal;

/**
 * @author Gavin King
 */
public class DataPoint {
	private long id;
	private int sequence;
	private BigDecimal x;
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
