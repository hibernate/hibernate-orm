/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.ejb.engine.spi;

import java.util.Iterator;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.event.spi.EventSource;
import org.hibernate.type.CollectionType;

/**
 * Because of CascadingAction constructor visibility
 * I need a packaged friendly subclass
 * TODO Get rid of it for 3.3
 * @author Emmanuel Bernard
 */
public abstract class EJB3CascadingAction extends CascadingAction {

    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(EntityManagerMessageLogger.class,
                                                                           EJB3CascadingAction.class.getName());
	/**
	 * @see org.hibernate.Session#persist(Object)
	 */
	public static final CascadingAction PERSIST_SKIPLAZY = new CascadingAction() {
		@Override
        public void cascade(EventSource session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled)
		throws HibernateException {
            LOG.trace("Cascading to persist: " + entityName);
			session.persist( entityName, child, (Map) anything );
		}
		@Override
        public Iterator getCascadableChildrenIterator(EventSource session, CollectionType collectionType, Object collection) {
			// persists don't cascade to uninitialized collections
			return CascadingAction.getLoadedElementsIterator( session, collectionType, collection );
		}
		@Override
        public boolean deleteOrphans() {
			return false;
		}
		@Override
        public boolean performOnLazyProperty() {
			return false;
		}
		@Override
        public String toString() {
			return "ACTION_PERSIST_SKIPLAZY";
		}
	};

}
