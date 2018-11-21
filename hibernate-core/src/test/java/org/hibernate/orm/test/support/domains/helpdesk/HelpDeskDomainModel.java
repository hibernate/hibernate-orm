/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.helpdesk;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.support.domains.DomainModel;

/**
 * @author Steve Ebersole
 */
public class HelpDeskDomainModel implements DomainModel {
	public static final HelpDeskDomainModel INSTANCE = new HelpDeskDomainModel();

	private static Class[] CLASSES = new Class[] {
			Status.class,
			Account.class
	};

	@Override
	public void applyDomainModel(MetadataSources sources) {
		for ( Class domainClass : CLASSES ) {
			sources.addAnnotatedClass( domainClass );
		}
	}
}
