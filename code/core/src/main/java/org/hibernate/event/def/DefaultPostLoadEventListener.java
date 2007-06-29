//$Id: DefaultPostLoadEventListener.java 7785 2005-08-08 23:24:44Z oneovthafew $
package org.hibernate.event.def;

import org.hibernate.classic.Lifecycle;
import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PostLoadEventListener;

/**
 * Call <tt>Lifecycle</tt> interface if necessary
 *
 * @author Gavin King
 */
public class DefaultPostLoadEventListener implements PostLoadEventListener {
	
	public void onPostLoad(PostLoadEvent event) {
		if ( event.getPersister().implementsLifecycle( event.getSession().getEntityMode() ) ) {
			//log.debug( "calling onLoad()" );
			( ( Lifecycle ) event.getEntity() ).onLoad( event.getSession(), event.getId() );
		}

	}
}
