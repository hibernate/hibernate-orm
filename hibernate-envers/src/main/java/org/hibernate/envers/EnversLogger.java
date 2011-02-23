/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers;

import static org.jboss.logging.Logger.Level.WARN;
import org.hibernate.HibernateLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Defines internationalized messages for this hibernate-envers, with IDs ranging from 25001 to 30000 inclusively. New messages must
 * be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger( projectCode = "HHH" )
public interface EnversLogger extends HibernateLogger {

    @LogMessage( level = WARN )
    @Message( value = "ValidTimeAuditStrategy is deprecated, please use ValidityAuditStrategy instead", id = 25001 )
    void validTimeAuditStrategyDeprecated();
}
