/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.metamodel.generics.embeddable;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractValueObject<V extends Comparable<V>> implements Serializable,
		Comparable<AbstractValueObject<V>> {
	public static final String VALUE = "value";

	@Column( name = "value_col" )
	private V value;

	protected AbstractValueObject() {
		super();
	}

	protected AbstractValueObject(final V value) {
		this.value = value;
	}

	@Override
	public int compareTo(final AbstractValueObject<V> object) {
		return value.compareTo( object.value );
	}
}
