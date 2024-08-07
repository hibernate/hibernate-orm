/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain;

import org.hibernate.testing.orm.domain.animal.AnimalDomainModel;
import org.hibernate.testing.orm.domain.contacts.ContactsDomainModel;
import org.hibernate.testing.orm.domain.gambit.GambitDomainModel;
import org.hibernate.testing.orm.domain.helpdesk.HelpDeskDomainModel;
import org.hibernate.testing.orm.domain.library.LibraryDomainModel;
import org.hibernate.testing.orm.domain.retail.RetailDomainModel;
import org.hibernate.testing.orm.domain.userguide.UserguideDomainModel;

/**
 * @author Steve Ebersole
 */
public enum StandardDomainModel {
	CONTACTS( ContactsDomainModel.INSTANCE ),
	ANIMAL( AnimalDomainModel.INSTANCE ),
	GAMBIT( GambitDomainModel.INSTANCE ),
	HELPDESK( HelpDeskDomainModel.INSTANCE ),
	RETAIL( RetailDomainModel.INSTANCE ),
	USERGUIDE( UserguideDomainModel.INSTANCE ),
	LIBRARY( LibraryDomainModel.INSTANCE );

	private final DomainModelDescriptor domainModel;

	StandardDomainModel(DomainModelDescriptor domainModel) {
		this.domainModel = domainModel;
	}

	public DomainModelDescriptor getDescriptor() {
		return domainModel;
	}
}
