//$Id: JACCPreDeleteEventListener.java 8702 2005-11-29 18:34:29Z kabkhan $
package org.hibernate.secure;

import javax.security.jacc.EJBMethodPermission;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.event.Initializable;
import org.hibernate.event.PreDeleteEvent;
import org.hibernate.event.PreDeleteEventListener;

/**
 * Check security before any deletion
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public class JACCPreDeleteEventListener implements PreDeleteEventListener, Initializable, JACCSecurityListener {
	private String contextID;

	public boolean onPreDelete(PreDeleteEvent event) {

		EJBMethodPermission deletePermission = new EJBMethodPermission(
				event.getPersister().getEntityName(),
				HibernatePermission.DELETE,
				null,
				null
		);

		JACCPermissions.checkPermission( event.getEntity().getClass(), contextID, deletePermission );

		return false;
	}

   public void initialize(Configuration cfg){
      contextID = cfg.getProperty(Environment.JACC_CONTEXTID);
   }
}
