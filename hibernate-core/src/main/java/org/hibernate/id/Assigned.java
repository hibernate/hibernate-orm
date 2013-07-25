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
package org.hibernate.id;
import java.io.Serializable;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.Type;

/**
 * <b>assigned</b><br>
 * <br>
 * An <tt>IdentifierGenerator</tt> that returns the current identifier assigned
 * to an instance.
 *
 * @author Gavin King
 */

public class Assigned implements IdentifierGenerator, Configurable {
	
	private String entityName;

	public Serializable generate(SessionImplementor session, Object obj) throws HibernateException {
		//TODO: cache the persister, this shows up in yourkit
		final Serializable id = session.getEntityPersister( entityName, obj ).getIdentifier( obj, session );
		if ( id == null ) {
			throw new IdentifierGenerationException(
					"ids for this class must be manually assigned before calling save(): " + entityName
			);
		}
		
		return id;
	}

	public void configure(Type type, Properties params, Dialect d, ClassLoaderService classLoaderService) throws MappingException {
		entityName = params.getProperty(ENTITY_NAME);
		if ( entityName == null ) {
			throw new MappingException("no entity name");
		}
	}
}






