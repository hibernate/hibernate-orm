/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.envers.internal.entities.RevisionTypeType;
import org.hibernate.service.ServiceRegistry;

/**
 * Envers specific TypeContributor
 *
 * @author Brett Meyer
 */
public class TypeContributorImpl implements TypeContributor {
	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final EnversService enversService = serviceRegistry.getService( EnversService.class );
		if ( !enversService.isEnabled() ) {
			return;
		}

		typeContributions.contributeType(
				new RevisionTypeType(),
				new String[] { RevisionTypeType.class.getName() }
		);
	}

}
