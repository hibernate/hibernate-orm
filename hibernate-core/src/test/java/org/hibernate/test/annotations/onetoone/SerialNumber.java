/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.onetoone;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class SerialNumber {
	private SerialNumberPk id;
	private String value;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof SerialNumber ) ) return false;

		final SerialNumber serialNumber = (SerialNumber) o;

		if ( !id.equals( serialNumber.id ) ) return false;

		return true;
	}

	public int hashCode() {
		return id.hashCode();
	}

	@Id
	public SerialNumberPk getId() {
		return id;
	}

	public void setId(SerialNumberPk id) {
		this.id = id;
	}

    @Column(name="`value`")
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
