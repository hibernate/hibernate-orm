/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.type.spi.TypeResolverTemplate;
import org.hibernate.boot.model.type.spi.TypeDefinitionRegistry;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Chris Cranford
 */
public class TypeDefinitionRegistryImpl implements TypeDefinitionRegistry {
	private static final Logger log = Logger.getLogger( TypeDefinitionRegistryImpl.class );

	private final TypeConfiguration typeConfiguration;
	private final Map<String, TypeDefinition> typeResolverTemplateMap = new HashMap<>();

	public TypeDefinitionRegistryImpl(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public TypeDefinition resolve(String typeName) {
		return typeResolverTemplateMap.get( typeName );
	}

	@Override
	public TypeDefinitionRegistry register(TypeDefinition typeDefinition) {
		return register( typeDefinition, DuplicationStrategy.OVERWRITE );
	}

	@Override
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
			if ( !typeResolverTemplateMap.containsKey( name ) ) {
				typeResolverTemplateMap.put( name, typeDefinition );
			}
		}
		else {
			final TypeResolverTemplate existing = typeResolverTemplateMap.put( name, typeDefinition );
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
