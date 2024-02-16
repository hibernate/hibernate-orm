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
import org.hibernate.type.descriptor.java.BasicJavaType;

import org.jboss.logging.Logger;

/**
 * Basic implementation of {@link TypeDefinitionRegistry}.
 *
 * @author Chris Cranford
 */
public class TypeDefinitionRegistryStandardImpl implements TypeDefinitionRegistry {
	private static final Logger log = Logger.getLogger( TypeDefinitionRegistryStandardImpl.class );

	private final TypeDefinitionRegistry parent;
	private final Map<String, TypeDefinition> typeDefinitionMap = new HashMap<>();

	public TypeDefinitionRegistryStandardImpl() {
		this( null );
	}

	public TypeDefinitionRegistryStandardImpl(TypeDefinitionRegistry parent) {
		this.parent = parent;
	}

	@Override
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

	@Override
	public TypeDefinition resolveAutoApplied(BasicJavaType<?> jtd) {
		// For now, check the definition map for a entry keyed by the JTD name.
		// Ultimately should maybe have TypeDefinition or the registry keep explicit track of
		// auto-applied defs
		if ( jtd.getJavaType() == null ) {
			return null;
		}

		return typeDefinitionMap.get( jtd.getTypeName() );
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
									"Attempted to overwrite registration [%s] for type definition.",
									name
							)
					);
				}
			}
		}
	}

	@Override
	public Map<String, TypeDefinition> copyRegistrationMap() {
		return new HashMap<>( typeDefinitionMap );
	}
}
