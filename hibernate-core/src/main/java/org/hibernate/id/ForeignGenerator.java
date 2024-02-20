/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static org.hibernate.engine.internal.ForeignKeys.getEntityIdentifierIfNotUnsaved;
import static org.hibernate.id.IdentifierGeneratorHelper.SHORT_CIRCUIT_INDICATOR;
import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.spi.NavigablePath.IDENTIFIER_MAPPER_PROPERTY;

/**
 * The legacy id generator named {@code foreign}.
 * <p>
 * An {@code Identifier} generator that uses the value of the id property of an
 * associated object.
 * <p>
 * One mapping parameter is required: {@value PROPERTY}.
 *
 * @author Gavin King
 *
 * @deprecated This remains around as an implementation detail of {@code hbm.xml} mappings.
 */
@Deprecated(since = "6", forRemoval = true)
public class ForeignGenerator implements IdentifierGenerator, StandardGenerator {
	private static final CoreMessageLogger LOG = messageLogger( ForeignGenerator.class );

	/**
	 * The parameter which specifies the property holding a reference to the associated object.
	 */
	public static final String PROPERTY = "property";

	private String entityName;
	private String propertyName;

	/**
	 * Getter for property 'entityName'.
	 *
	 * @return Value for property 'entityName'.
	 */
	public String getEntityName() {
		return entityName;
	}

	/**
	 * Getter for property 'propertyName'.
	 *
	 * @return Value for property 'propertyName'.
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Getter for property 'role'.  Role is the {@link #getPropertyName property name} qualified by the
	 * {@link #getEntityName entity name}.
	 *
	 * @return Value for property 'role'.
	 */
	public String getRole() {
		return getEntityName() + '.' + getPropertyName();
	}


	@Override
	public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) throws MappingException {
		propertyName = parameters.getProperty( PROPERTY );
		entityName = parameters.getProperty( ENTITY_NAME );
		if ( propertyName==null ) {
			throw new MappingException( "param named \"property\" is required for foreign id generation strategy" );
		}
	}

	@Override
	public Object generate(SharedSessionContractImplementor sessionImplementor, Object object) {
		final EntityPersister entityDescriptor =
				sessionImplementor.getFactory().getMappingMetamodel()
						.getEntityDescriptor( entityName );
		final Object associatedObject = entityDescriptor.getPropertyValue( object, propertyName );
		if ( associatedObject == null ) {
			throw new IdentifierGenerationException(
					"attempted to assign id from null one-to-one property [" + getRole() + "]"
			);
		}

		final EntityType foreignValueSourceType;
		final Type propertyType = entityDescriptor.getPropertyType( propertyName );
		if ( propertyType.isEntityType() ) {
			// the normal case
			foreignValueSourceType = (EntityType) propertyType;
		}
		else {
			// try identifier mapper
			foreignValueSourceType = (EntityType)
					entityDescriptor.getPropertyType( IDENTIFIER_MAPPER_PROPERTY + "." + propertyName );
		}

		Object id;
		final String associatedEntityName = foreignValueSourceType.getAssociatedEntityName();
		try {
			id = getEntityIdentifierIfNotUnsaved( associatedEntityName, associatedObject, sessionImplementor );
		}
		catch (TransientObjectException toe) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"ForeignGenerator detected a transient entity [%s]",
						associatedEntityName
				);
			}
			if ( sessionImplementor.isSessionImplementor() ) {
				id = sessionImplementor.asSessionImplementor().save( associatedEntityName, associatedObject );
			}
			else if ( sessionImplementor.isStatelessSession() ) {
				id = sessionImplementor.asStatelessSession().insert( associatedEntityName, associatedObject );
			}
			else {
				throw new IdentifierGenerationException("sessionImplementor is neither Session nor StatelessSession");
			}
		}

		if ( sessionImplementor.isSessionImplementor()
				&& sessionImplementor.asSessionImplementor().contains( entityName, object ) ) {
			//abort the save (the object is already saved by a circular cascade)
			return SHORT_CIRCUIT_INDICATOR;
			//throw new IdentifierGenerationException("save associated object first, or disable cascade for inverse association");
		}
		return id;
	}
}
