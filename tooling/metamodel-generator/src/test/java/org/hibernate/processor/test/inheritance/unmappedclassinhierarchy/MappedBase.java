/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.inheritance.unmappedclassinhierarchy;

import java.util.Date;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class MappedBase {
	private Date creationDate;
	private Date updatedOn;

	protected MappedBase(final Date date) {
		this.creationDate = date;
		this.updatedOn = date;
	}

	protected MappedBase() {
		this( new Date() );
	}
}
