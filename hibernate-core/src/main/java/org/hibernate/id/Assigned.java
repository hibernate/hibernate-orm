/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * An {@link IdentifierGenerator} that returns the current identifier assigned
 * to an instance.
 *
 * @author Gavin King
 *
 * @implNote This also implements the {@code assigned} generation type in {@code hbm.xml} mappings.
 */
public class Assigned implements IdentifierGenerator {
	private final String entityName;

	public Assigned(String entityName) {
		this.entityName = entityName;
	}

	@Override
	public boolean allowAssignedIdentifiers() {
		return true;
	}

	public Object generate(SharedSessionContractImplementor session, Object owner) throws HibernateException {
		//TODO: cache the persister, this shows up in yourkit
		final Object id = session.getEntityPersister( entityName, owner ).getIdentifier( owner, session );
		if ( id == null ) {
			throw new IdentifierGenerationException( "Identifier for entity '" + entityName
					+ "' must be manually assigned before making the entity persistent" );
		}
		return id;
	}
}
