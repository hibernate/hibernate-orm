/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.annotations;

import static org.jboss.logging.Logger.Level.WARN;
import org.hibernate.annotations.OrderBy;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Bind a set.
 *
 * @author Matthew Inger
 */
public class SetBinder extends CollectionBinder {
    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                SetBinder.class.getPackage().getName());

	public SetBinder() {
	}

	public SetBinder(boolean sorted) {
		super( sorted );
	}

	@Override
    protected Collection createCollection(PersistentClass persistentClass) {
		return new org.hibernate.mapping.Set( getMappings(), persistentClass );
	}

	@Override
    public void setSqlOrderBy(OrderBy orderByAnn) {
		// *annotation* binder, jdk 1.5, ... am i missing something?
		if ( orderByAnn != null ) {
            if (Environment.jvmSupportsLinkedHashCollections()) super.setSqlOrderBy(orderByAnn);
            else LOG.orderByAttributeIgnored();
		}
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = WARN )
        @Message( value = "Attribute \"order-by\" ignored in JDK1.3 or less" )
        void orderByAttributeIgnored();
    }
}
