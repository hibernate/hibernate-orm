/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
