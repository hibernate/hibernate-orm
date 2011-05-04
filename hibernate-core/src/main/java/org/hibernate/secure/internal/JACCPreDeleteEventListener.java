/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.secure.internal;

import javax.security.jacc.EJBMethodPermission;

import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;

/**
 * Check security before any deletion
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public class JACCPreDeleteEventListener implements PreDeleteEventListener, JACCSecurityListener {
	private final String contextId;

	public JACCPreDeleteEventListener(String contextId) {
		this.contextId = contextId;
	}

	public boolean onPreDelete(PreDeleteEvent event) {
		final EJBMethodPermission deletePermission = new EJBMethodPermission(
				event.getPersister().getEntityName(),
				HibernatePermission.DELETE,
				null,
				null
		);
		JACCPermissions.checkPermission( event.getEntity().getClass(), contextId, deletePermission );
		return false;
	}

}
