/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.components.mappedsuperclass;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.MappedSuperclass;

/**
 * @author Jakob Braeuchi.
 */

@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class AbstractEmbeddable {

	/**
	 * Initial Value
	 */
	protected static final int UNDEFINED = -1;

	private int code = UNDEFINED;


	protected AbstractEmbeddable() {
		this( UNDEFINED );
	}

	/**
	 * Constructor with code
	 */
	public AbstractEmbeddable(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + code;
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
		AbstractEmbeddable other = (AbstractEmbeddable) obj;
		if ( code != other.code ) {
			return false;
		}
		return true;
	}
}