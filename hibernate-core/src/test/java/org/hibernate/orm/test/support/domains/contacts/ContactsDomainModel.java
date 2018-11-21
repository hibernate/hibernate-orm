/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.contacts;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.support.domains.DomainModel;

/**
 * @author Steve Ebersole
 */
public class ContactsDomainModel implements DomainModel {
	public static ContactsDomainModel INSTANCE = new ContactsDomainModel();

	private static final Class[] CLASSES = new Class[] {
			Address.class,
			PhoneNumber.class,
			Contact.class
	};

	public static void applyContactsModel(MetadataSources sources) {
		for ( Class domainClass : CLASSES ) {
			sources.addAnnotatedClass( domainClass );
		}
	}

	private ContactsDomainModel() {
	}

	@Override
	public void applyDomainModel(MetadataSources sources) {
		applyContactsModel( sources );
	}
}
