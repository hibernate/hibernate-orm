/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.contacts;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.domain.AbstractDomainModelDescriptor;

/**
 * @author Steve Ebersole
 */
public class ContactsDomainModel extends AbstractDomainModelDescriptor {
	public static ContactsDomainModel INSTANCE = new ContactsDomainModel();

	public static void applyContactsModel(MetadataSources sources) {
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
