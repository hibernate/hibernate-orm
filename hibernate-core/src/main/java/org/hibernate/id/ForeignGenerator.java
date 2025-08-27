/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;

import static org.hibernate.id.IdentifierGeneratorHelper.getForeignId;

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
public class ForeignGenerator implements IdentifierGenerator {

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
	public void configure(GeneratorCreationContext creationContext, Properties parameters) throws MappingException {
		propertyName = parameters.getProperty( PROPERTY );
		entityName = parameters.getProperty( ENTITY_NAME );
		if ( propertyName==null ) {
			throw new MappingException( "param named \"property\" is required for foreign id generation strategy" );
		}
	}

	@Override
	public Object generate(SharedSessionContractImplementor sessionImplementor, Object object) {
		return getForeignId( entityName, propertyName, sessionImplementor, object );
	}

	@Override
	public boolean allowAssignedIdentifiers() {
		return true;
	}
}
