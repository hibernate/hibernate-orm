/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.helpdesk;

import org.hibernate.testing.orm.domain.AbstractDomainModelDescriptor;

/**
 * @author Steve Ebersole
 */
public class HelpDeskDomainModel extends AbstractDomainModelDescriptor {
	public static final HelpDeskDomainModel INSTANCE = new HelpDeskDomainModel();

	public HelpDeskDomainModel() {
		super(
				Status.class,
				Account.class,
				Ticket.class,
				Incident.class
		);
	}
}
