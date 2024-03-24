/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * An {@link IdentifierGenerator} that returns the current identifier assigned
 * to an instance.
 *
 * @author Gavin King
 *
 * @implNote This also implements the {@code assigned} generation type in {@code hbm.xml} mappings.
 */
public class Assigned implements IdentifierGenerator, StandardGenerator {
	private String entityName;

	public Object generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {
		//TODO: cache the persister, this shows up in yourkit
		final Object id = session.getEntityPersister( entityName, obj ).getIdentifier( obj, session );
		if ( id == null ) {
			throw new IdentifierGenerationException( "Identifier for entity '" + entityName
					+ "' must be manually assigned before making the entity persistent" );
		}
		return id;
	}

	@Override
	public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) throws MappingException {
		entityName = parameters.getProperty( ENTITY_NAME );
		if ( entityName == null ) {
			throw new MappingException("no entity name");
		}
	}
}
