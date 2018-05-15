/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * <b>foreign</b><br>
 * <br>
 * An <tt>Identifier</tt> generator that uses the value of the id property of an
 * associated object<br>
 * <br>
 * One mapping parameter is required: property.
 *
 * @author Gavin King
 */
public class ForeignGenerator implements IdentifierGenerator, Configurable {
	private static final CoreMessageLogger LOG = messageLogger( ForeignGenerator.class );

	private String entityName;
	private String propertyName;
	private JavaTypeDescriptor javaTypeDescriptor;

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
	public void configure(JavaTypeDescriptor javaTypeDescriptor, Properties params, ServiceRegistry serviceRegistry)
			throws MappingException {
		this.javaTypeDescriptor = javaTypeDescriptor;
		propertyName = params.getProperty( "property" );
		entityName = params.getProperty( ENTITY_NAME );
		if ( propertyName==null ) {
			throw new MappingException( "param named \"property\" is required for foreign id generation strategy" );
		}

	}

	@Override
	public Object generate(SharedSessionContractImplementor sessionImplementor, Object object) {
		// needs to be a Session for the #save and #contains calls below...
		final Session session = ( Session ) sessionImplementor;

		final EntityTypeDescriptor descriptor = sessionImplementor.getFactory().getMetamodel().findEntityDescriptor( entityName );
		Object associatedObject = descriptor.getPropertyValue( object, propertyName );
		if ( associatedObject == null ) {
			throw new IdentifierGenerationException(
					"attempted to assign id from null one-to-one property [" + getRole() + "]"
			);
		}

		final String entityName = retrieveEntityName( descriptor );

		Object id;
		try {
			id = ForeignKeys.getEntityIdentifierIfNotUnsaved(
					entityName,
					associatedObject,
					sessionImplementor
			);
		}
		catch (TransientObjectException toe) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"ForeignGenerator detected a transient entity [%s]",
						entityName
				);
			}
			id = session.save( entityName, associatedObject );
		}

		if ( session.contains( entityName, object ) ) {
			//abort the save (the object is already saved by a circular cascade)
			return IdentifierGeneratorHelper.SHORT_CIRCUIT_INDICATOR;
			//throw new IdentifierGenerationException("save associated object first, or disable cascade for inverse association");
		}
		return id;
	}

	private String retrieveEntityName(EntityTypeDescriptor descriptor) {
		String entityName;
		final PersistentAttributeDescriptor attribute = descriptor.findPersistentAttribute( propertyName );
		if ( attribute.getPersistenceType() == javax.persistence.metamodel.Type.PersistenceType.ENTITY ) {
			// the normal case
			entityName = attribute.getContainer().getNavigableName();
		}
		else {
			// try identifier mapper
			entityName = descriptor.findPersistentAttribute( NavigablePath.IDENTIFIER_MAPPER_PROPERTY + "." + propertyName ).getContainer().getNavigableName();
		}
		return entityName;
	}
}
