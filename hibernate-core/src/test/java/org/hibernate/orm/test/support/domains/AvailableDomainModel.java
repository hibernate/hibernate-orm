/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains;

import org.hibernate.orm.test.support.domains.contacts.ContactsDomainModel;
import org.hibernate.orm.test.support.domains.gambit.GambitDomainModel;
import org.hibernate.orm.test.support.domains.helpdesk.HelpDeskDomainModel;
import org.hibernate.orm.test.support.domains.retail.RetailDomainModel;

/**
 * @author Steve Ebersole
 */
public enum AvailableDomainModel {
	CONTACTS( ContactsDomainModel.INSTANCE ),
	GAMBIT( GambitDomainModel.INSTANCE ),
	HELPDESK( HelpDeskDomainModel.INSTANCE ),
	RETAIL( RetailDomainModel.INSTANCE );

	private final DomainModel domainModel;

	AvailableDomainModel(DomainModel domainModel) {
		this.domainModel = domainModel;
	}

	public DomainModel getDomainModel() {
		return domainModel;
	}
}
