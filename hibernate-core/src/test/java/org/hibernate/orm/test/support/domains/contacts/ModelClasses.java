/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.contacts;

import org.hibernate.boot.MetadataSources;

/**
 * @author Steve Ebersole
 */
public class ModelClasses {
	public static final Class[] CLASSES = new Class[] {
			Address.class,
			PhoneNumber.class,
			Contact.class
	};

	public static void applyContactsModel(MetadataSources sources) {
		for ( Class domainClass : CLASSES ) {
			sources.addAnnotatedClass( domainClass );
		}
	}
}
