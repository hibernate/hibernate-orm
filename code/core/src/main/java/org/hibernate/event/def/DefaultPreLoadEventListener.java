//$Id: DefaultPreLoadEventListener.java 7785 2005-08-08 23:24:44Z oneovthafew $
package org.hibernate.event.def;

import org.hibernate.event.PreLoadEvent;
import org.hibernate.event.PreLoadEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Called before injecting property values into a newly 
 * loaded entity instance.
 *
 * @author Gavin King
 */
public class DefaultPreLoadEventListener implements PreLoadEventListener {
	
	public void onPreLoad(PreLoadEvent event) {
		EntityPersister persister = event.getPersister();
		event.getSession()
			.getInterceptor()
			.onLoad( 
					event.getEntity(), 
					event.getId(), 
					event.getState(), 
					persister.getPropertyNames(), 
					persister.getPropertyTypes() 
				);
	}
	
}
