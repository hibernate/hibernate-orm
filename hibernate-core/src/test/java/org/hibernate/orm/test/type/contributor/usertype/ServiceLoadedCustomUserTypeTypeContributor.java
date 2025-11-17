/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor.usertype;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.orm.test.mapping.basic.MonetaryAmountUserType;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Jan Schatteman
 */
public class ServiceLoadedCustomUserTypeTypeContributor implements TypeContributor {

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		typeContributions.contributeType( MonetaryAmountUserType.INSTANCE );
	}
}
