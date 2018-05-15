/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.collections.components;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Andrea Boriero
 */
@Embeddable
public class Image {
	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String size;

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Image image = (Image) o;
		return Objects.equals( name, image.name ) &&
				Objects.equals( size, image.size );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, size );
	}
}
