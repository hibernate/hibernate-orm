/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.formulajoin;

import java.io.Serializable;

public class Id implements Serializable {
	private String mandant;

	private int id;

	public Id() {
		super();
	}

	public Id(String mandant, int id) {
		super();
		this.mandant = mandant;
		this.id = id;
	}

	public String getMandant() {
		return mandant;
	}

	public void setMandant(String mandant) {
		this.mandant = mandant;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ( ( mandant == null ) ? 0 : mandant.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		Id other = (Id) obj;
		if ( id != other.id ) {
			return false;
		}
		if ( mandant == null ) {
			if ( other.mandant != null ) {
				return false;
			}
		}
		else if ( !mandant.equals( other.mandant ) ) {
			return false;
		}
		return true;
	}
}
