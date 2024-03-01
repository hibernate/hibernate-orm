/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.inheritance.unmappedclassinhierarchy;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;

@Entity
@Access(AccessType.FIELD)
public class SubB extends NormalExtendsMapped {
	protected String street;

	public String getStreet() {
		return street;
	}
}
