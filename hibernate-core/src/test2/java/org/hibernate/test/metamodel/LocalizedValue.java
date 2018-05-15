/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.metamodel;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class LocalizedValue implements ILocalizable {

	@Column(name = "val")
	private String value;

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( ( o == null ) || ( getClass() != o.getClass() ) ) {
			return false;
		}
		LocalizedValue that = ( (LocalizedValue) o );
		return Objects.equals( value, that.value );
	}

	@Override
	public int hashCode() {
		return Objects.hash( value );
	}
}
