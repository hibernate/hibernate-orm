/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.secure;

import javax.security.jacc.EJBMethodPermission;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.event.Initializable;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;

/**
 * Check security before any update
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision: 8702 $
 */
public class JACCPreUpdateEventListener implements PreUpdateEventListener, Initializable, JACCSecurityListener {
	private String contextID;

	public boolean onPreUpdate(PreUpdateEvent event) {

		EJBMethodPermission updatePermission = new EJBMethodPermission(
				event.getPersister().getEntityName(),
				HibernatePermission.UPDATE,
				null,
				null
		);

		JACCPermissions.checkPermission( event.getEntity().getClass(), contextID, updatePermission );

		return false;
	}


   public void initialize(Configuration cfg){
      contextID = cfg.getProperty(Environment.JACC_CONTEXTID);
   }
}
