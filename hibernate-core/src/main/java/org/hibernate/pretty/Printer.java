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
package org.hibernate.pretty;

import static org.jboss.logging.Logger.Level.DEBUG;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.intercept.LazyPropertyInitializer;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Renders entities to a nicely readable string.
 * @author Gavin King
 */
public final class Printer {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class, Printer.class.getPackage().getName());

    private SessionFactoryImplementor factory;

	/**
	 * @param entity an actual entity object, not a proxy!
	 */
	public String toString(Object entity, EntityMode entityMode) throws HibernateException {

		// todo : this call will not work for anything other than pojos!
		ClassMetadata cm = factory.getClassMetadata( entity.getClass() );

		if ( cm==null ) return entity.getClass().getName();

		Map result = new HashMap();

		if ( cm.hasIdentifierProperty() ) {
			result.put(
				cm.getIdentifierPropertyName(),
				cm.getIdentifierType().toLoggableString( cm.getIdentifier( entity, entityMode ), factory )
			);
		}

		Type[] types = cm.getPropertyTypes();
		String[] names = cm.getPropertyNames();
		Object[] values = cm.getPropertyValues( entity, entityMode );
		for ( int i=0; i<types.length; i++ ) {
			if ( !names[i].startsWith("_") ) {
				String strValue = values[i]==LazyPropertyInitializer.UNFETCHED_PROPERTY ?
					values[i].toString() :
					types[i].toLoggableString( values[i], factory );
				result.put( names[i], strValue );
			}
		}
		return cm.getEntityName() + result.toString();
	}

	public String toString(Type[] types, Object[] values) throws HibernateException {
		List list = new ArrayList( types.length * 5 );
		for ( int i=0; i<types.length; i++ ) {
			if ( types[i]!=null ) list.add( types[i].toLoggableString( values[i], factory ) );
		}
		return list.toString();
	}

	public String toString(Map namedTypedValues) throws HibernateException {
		Map result = new HashMap();
		Iterator iter = namedTypedValues.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			TypedValue tv = (TypedValue) me.getValue();
			result.put( me.getKey(), tv.getType().toLoggableString( tv.getValue(), factory ) );
		}
		return result.toString();
	}

	public void toString(Iterator iter, EntityMode entityMode) throws HibernateException {
        if (!LOG.isDebugEnabled() || !iter.hasNext()) return;
        LOG.listingEntities();
		int i=0;
		while ( iter.hasNext() ) {
			if (i++>20) {
                LOG.more();
				break;
			}
            LOG.debug(toString(iter.next(), entityMode));
		}
	}

	public Printer(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = DEBUG )
        @Message( value = "Listing entities:" )
        void listingEntities();

        @LogMessage( level = DEBUG )
        @Message( value = "More......" )
        void more();
    }
}
