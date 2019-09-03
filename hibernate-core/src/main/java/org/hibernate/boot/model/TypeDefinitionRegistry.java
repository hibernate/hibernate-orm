/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

/**
 * @author Chris Cranford
 */
public class TypeDefinitionRegistry {
	private static final Logger log = Logger.getLogger( TypeDefinitionRegistry.class );
	private final TypeDefinitionRegistry parent;

	public enum DuplicationStrategy {
		KEEP,
		OVERWRITE,
		DISALLOW
	}

	private final Map<String, TypeDefinition> typeDefinitionMap = new HashMap<>();

	public TypeDefinitionRegistry() {
		this( null );
	}

	public TypeDefinitionRegistry(TypeDefinitionRegistry parent) {
		this.parent = parent;
	}

	public TypeDefinition resolve(String typeName) {
		final TypeDefinition localDefinition = typeDefinitionMap.get( typeName );
		if ( localDefinition != null ) {
			return localDefinition;
		}

		if ( parent != null ) {
			return parent.resolve( typeName );
		}

		return null;
	}

	public TypeDefinitionRegistry register(TypeDefinition typeDefinition) {
		return register( typeDefinition, DuplicationStrategy.OVERWRITE );
	}

	public TypeDefinitionRegistry register(TypeDefinition typeDefinition, DuplicationStrategy duplicationStrategy) {
		if ( typeDefinition == null ) {
			throw new IllegalArgumentException( "TypeDefinition to register cannot be null" );
		}

		if ( typeDefinition.getTypeImplementorClass() == null ) {
			throw new IllegalArgumentException( "TypeDefinition to register cannot define null #typeImplementorClass" );
		}

		if ( !StringHelper.isEmpty( typeDefinition.getName() ) ) {
			register( typeDefinition.getName(), typeDefinition, duplicationStrategy );
		}

		if ( typeDefinition.getRegistrationKeys() != null ) {
			for ( String registrationKey : typeDefinition.getRegistrationKeys() ) {
				register( registrationKey, typeDefinition, duplicationStrategy );
			}
		}

		return this;
	}

	private void register(String name, TypeDefinition typeDefinition, DuplicationStrategy duplicationStrategy) {
		if ( duplicationStrategy == DuplicationStrategy.KEEP ) {
			if ( !typeDefinitionMap.containsKey( name ) ) {
				typeDefinitionMap.put( name, typeDefinition );
			}
		}
		else {
			final TypeDefinition existing = typeDefinitionMap.put( name, typeDefinition );
			if ( existing != null && existing != typeDefinition ) {
				if ( duplicationStrategy == DuplicationStrategy.OVERWRITE ) {
					log.debugf( "Overwrote existing registration [%s] for type definition.", name );
				}
				else {
					throw new IllegalArgumentException(
							String.format(
									Locale.ROOT,
									"Attempted to ovewrite registration [%s] for type definition.",
									name
							)
					);
				}
			}
		}
	}
}
