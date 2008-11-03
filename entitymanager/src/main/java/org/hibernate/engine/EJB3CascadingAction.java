//$Id$
package org.hibernate.engine;

import java.util.Map;
import java.util.Iterator;

import org.hibernate.event.EventSource;
import org.hibernate.HibernateException;
import org.hibernate.type.CollectionType;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Because of CascadingAction constructor visibility
 * I need a packaged friendly subclass
 * TODO Get rid of it for 3.3
 * @author Emmanuel Bernard
 */
public abstract class EJB3CascadingAction extends CascadingAction {
	private static Logger log = LoggerFactory.getLogger( CascadingAction.class );
	/**
	 * @see org.hibernate.Session#persist(Object)
	 */
	public static final CascadingAction PERSIST_SKIPLAZY = new CascadingAction() {
		public void cascade(EventSource session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled)
		throws HibernateException {
			log.trace( "cascading to persist: {}", entityName );
			session.persist( entityName, child, (Map) anything );
		}
		public Iterator getCascadableChildrenIterator(EventSource session, CollectionType collectionType, Object collection) {
			// persists don't cascade to uninitialized collections
			return CascadingAction.getLoadedElementsIterator( session, collectionType, collection );
		}
		public boolean deleteOrphans() {
			return false;
		}
		public boolean performOnLazyProperty() {
			return false;
		}
		public String toString() {
			return "ACTION_PERSIST_SKIPLAZY";
		}
	};
	
}
