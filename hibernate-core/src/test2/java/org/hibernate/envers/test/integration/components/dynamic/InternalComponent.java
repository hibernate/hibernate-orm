/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.components.dynamic;

public class InternalComponent {

	private String property;

	public InternalComponent() {
	}

	public InternalComponent(String property) {
		this.property = property;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof InternalComponent ) ) {
			return false;
		}

		InternalComponent that = (InternalComponent) o;

		if ( property != null ? !property.equals( that.property ) : that.property != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return property != null ? property.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "InternalComponent{" +
				"property='" + property + '\'' +
				'}';
	}
}
