/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
