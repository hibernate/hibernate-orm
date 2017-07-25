/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

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
	public Serializable generate(SharedSessionContractImplementor sessionImplementor, Object object) {
		// needs to be a Session for the #save and #contains calls below...
		final Session session = ( Session ) sessionImplementor;

		final EntityDescriptor persister = sessionImplementor.getFactory().getTypeConfiguration().findEntityDescriptor( entityName );
		Object associatedObject = persister.getPropertyValue( object, propertyName );
		if ( associatedObject == null ) {
			throw new IdentifierGenerationException(
					"attempted to assign id from null one-to-one property [" + getRole() + "]"
			);
		}

		final EntityType foreignValueSourceType;
		final Type propertyType = persister.getPropertyType( propertyName );
		if ( propertyType.getgetClassification().equals( Type.Classification.ENTITY ) ) {
			// the normal case
			foreignValueSourceType = (EntityType) propertyType;
		}
		else {
			// try identifier mapper
			foreignValueSourceType = (EntityType) persister.getPropertyType( PropertyPath.IDENTIFIER_MAPPER_PROPERTY + "." + propertyName );
		}

		Serializable id;
		try {
			id = ForeignKeys.getEntityIdentifierIfNotUnsaved(
					foreignValueSourceType.getAssociatedEntityName(),
					associatedObject,
					sessionImplementor
			);
		}
		catch (TransientObjectException toe) {
			id = session.save( foreignValueSourceType.getAssociatedEntityName(), associatedObject );
		}

		if ( session.contains( entityName, object ) ) {
			//abort the save (the object is already saved by a circular cascade)
			return IdentifierGeneratorHelper.SHORT_CIRCUIT_INDICATOR;
			//throw new IdentifierGenerationException("save associated object first, or disable cascade for inverse association");
		}
		return id;
	}
}
