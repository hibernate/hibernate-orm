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
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

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
	private EntityTypeDescriptor entityDescriptor;

	@Override
	public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		if ( entityDescriptor == null ) {
			entityDescriptor = session.getEntityDescriptor( entityName, object );
		}

		final Object id = entityDescriptor.getIdentifier( object, session );
		if ( id == null ) {
			throw new IdentifierGenerationException(
					"ids for this class must be manually assigned before calling save(): " + entityName
			);
		}
		
		return id;
	}

	@Override
	public void configure(JavaTypeDescriptor javaTypeDescriptor, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		entityName = params.getProperty( ENTITY_NAME );
		if ( entityName == null ) {
			throw new MappingException("no entity name");
		}
	}
}
