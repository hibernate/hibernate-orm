/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.contacts;

import org.hibernate.boot.pipeline.internal.source.MappingSources;

import org.hibernate.testing.orm.domain.AbstractDomainModelDescriptor;

/**
 * @author Steve Ebersole
 */
public class ContactsDomainModel extends AbstractDomainModelDescriptor {
	public static ContactsDomainModel INSTANCE = new ContactsDomainModel();

	public static void applyContactsModel(MappingSources sources) {
		INSTANCE.applyDomainModel( sources );
	}

	private ContactsDomainModel() {
		super(
				Address.class,
				PhoneNumber.class,
				Contact.class
		);
	}
}
