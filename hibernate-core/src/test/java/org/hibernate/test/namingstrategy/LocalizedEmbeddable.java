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
package org.hibernate.test.namingstrategy;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class LocalizedEmbeddable implements Serializable, Cloneable {

	private static final long serialVersionUID = -8539302606114365372L;

	private String name;
	private String description;

	/**
	 * Empty Constructor
	 */
	public LocalizedEmbeddable() {
		super();
	}

	/**
	 * @param name
	 * @param description
	 */
	public LocalizedEmbeddable(String name, String description) {
		super();
		this.name = name;
		this.description = description;
	}

	@Column(name = "NAME", nullable = false, length = 255)
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "DESCRIPTION", nullable = false, length = 2500)
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public LocalizedEmbeddable clone() {
		return new LocalizedEmbeddable( this.name, this.description );
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( description == null ) ? 0 : description.hashCode() );
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		LocalizedEmbeddable other = (LocalizedEmbeddable) obj;
		if ( description == null ) {
			if ( other.description != null )
				return false;
		}
		else if ( !description.equals( other.description ) )
			return false;
		if ( name == null ) {
			if ( other.name != null )
				return false;
		}
		else if ( !name.equals( other.name ) )
			return false;
		return true;
	}
}
