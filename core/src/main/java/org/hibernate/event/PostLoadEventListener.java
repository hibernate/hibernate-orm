//$Id: PostLoadEventListener.java 5006 2004-12-19 20:15:13Z oneovthafew $
package org.hibernate.event;

import java.io.Serializable;

/**
 * Occurs after an an entity instance is fully loaded.
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public interface PostLoadEventListener extends Serializable {
	public void onPostLoad(PostLoadEvent event);
}
