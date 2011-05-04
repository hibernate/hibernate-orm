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

import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;

/**
 * Check security before an insertion
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public class JACCPreInsertEventListener implements PreInsertEventListener, JACCSecurityListener {
	private final String contextId;

	public JACCPreInsertEventListener(String contextId) {
		this.contextId = contextId;
	}

	public boolean onPreInsert(PreInsertEvent event) {
		final EJBMethodPermission insertPermission = new EJBMethodPermission(
				event.getPersister().getEntityName(),
				HibernatePermission.INSERT,
				null,
				null
		);
		JACCPermissions.checkPermission( event.getEntity().getClass(), contextId, insertPermission );
		return false;
	}
}
