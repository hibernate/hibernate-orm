/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;
import org.hibernate.util.ReflectHelper;

/**
 * Acts as the contract for getting types and as the mediator between {@link BasicTypeRegistry} and {@link TypeFactory}.
 *
 * @author Steve Ebersole
 */
public class TypeResolver implements Serializable {
	private final BasicTypeRegistry basicTypeRegistry;
	private final TypeFactory typeFactory;

	// Need to keep track of the "global" type overrides in case there are dialect-specific overrides.
	// Dialect-specific types must be applied before "global" overrides are applied.
	// Unfortunately, dialect-specific types are not known until this TypeResolver is scoped...
	private final List<BasicType> typeOverrides;

	// If this TypeResolver is scoped to a SessionFactoryImplementor with
	// dialect-specific type overrides, scopedTypeRegistry will be initialized,
	// dialect-specific type overrides will be applied, followed by "global"
	// type overrides.
	private BasicTypeRegistry scopedTypeRegistry;

	public TypeResolver() {
		this(  new BasicTypeRegistry(), new TypeFactory() );
	}

	public TypeResolver(BasicTypeRegistry basicTypeRegistry, TypeFactory typeFactory) {
		this.basicTypeRegistry = basicTypeRegistry;
		this.typeOverrides = new ArrayList<BasicType>();
		this.typeFactory = typeFactory;
	}

	public TypeResolver scope(SessionFactoryImplementor factory) {
		typeFactory.injectSessionFactory( factory );
		// if there was a scopedTypeRegistry left from the last time this
		// TypeResolver was scoped, then set it to null;
		scopedTypeRegistry = null;
		BasicTypeRegistry registry = basicTypeRegistry;
		List<BasicType> dialectTypeOverrides = factory.getDialect().getTypeOverrides();
		if ( factory != null && ! dialectTypeOverrides.isEmpty() ) {
			// scoping to a factory with dialect-specific type overrides;
			// create a new scopedTypeRegistry and override dialect-specific types
			// before overriding the "global" type overrides;
			scopedTypeRegistry = new BasicTypeRegistry();
			registerTypeOverrides( scopedTypeRegistry, dialectTypeOverrides );
			registerTypeOverrides( scopedTypeRegistry, typeOverrides );
			registry = scopedTypeRegistry;
		}
		return new TypeResolver( registry.shallowCopy(), typeFactory );
	}

	public void registerTypeOverride(BasicType type) {
		basicTypeRegistry.register( type );
		typeOverrides.add( type );
	}

	public void registerTypeOverride(UserType type, String[] keys) {
		basicTypeRegistry.register( type, keys );
		typeOverrides.add( new CustomType( type, keys ) );
	}

	public void registerTypeOverride(CompositeUserType type, String[] keys) {
		basicTypeRegistry.register( type, keys );
		typeOverrides.add( new CompositeCustomType( type, keys ) );
	}

	private static void registerTypeOverrides(BasicTypeRegistry typeRegistry, List<BasicType> typeOverrides) {
		for ( BasicType typeOverride : typeOverrides ) {
			typeRegistry.register( typeOverride );
		}
	}

	public TypeFactory getTypeFactory() {
		return typeFactory;
	}

	/**
	 * Locate a Hibernate {@linkplain BasicType basic type} given (one of) its registration names;
	 * if scoped to a {@link SessionFactoryImplementor}, the scoped type is returned.
	 *
	 * @param name The registration name
	 *
	 * @return The registered type
	 */
	public BasicType basic(String name) {
		return scopedTypeRegistry == null ?
				basicTypeRegistry.getRegisteredType( name ) :
				scopedTypeRegistry.getRegisteredType( name );
	}

	/**
	 * See {@link #heuristicType(String, Properties)}
	 *
	 * @param typeName The name (see heuristic algorithm discussion on {@link #heuristicType(String, Properties)}).
	 * 
	 * @return The deduced type; may be null.
	 *
	 * @throws MappingException Can be thrown from {@link #heuristicType(String, Properties)}
	 */
	public Type heuristicType(String typeName) throws MappingException {
		return heuristicType( typeName, null );
	}

	/**
	 * Uses heuristics to deduce the proper {@link Type} given a string naming the type or Java class.
	 * <p/>
	 * The search goes as follows:<ol>
	 * 	<li>search for a basic type with 'typeName' as a registration key</li>
	 * 	<li>
	 * 		look for 'typeName' as a class name and<ol>
	 *			<li>if it names a {@link Type} implementor, return an instance</li>
	 *			<li>if it names a {@link CompositeUserType} or a {@link UserType}, return an instance of class wrapped intot the appropriate {@link Type} adapter</li>
	 * 			<li>if it implements {@link Lifecycle}, return the corresponding entity type</li>
	 * 			<li>if it implements {@link Serializable}, return the corresponding serializable type</li>
	 * 		</ol>
	 * 	</li>
	 * </ol>
	 *
	 * @param typeName The name (see heuristic algorithm above).
	 * @param parameters Any parameters for the type.  Only applied if built!
	 *
	 * @return The deduced type; may be null.
	 *
	 * @throws MappingException Indicates a problem attempting to resolve 'typeName' as a {@link Class}
	 */
	public Type heuristicType(String typeName, Properties parameters) throws MappingException {
		Type type = basic( typeName );
		if ( type != null ) {
			return type;
		}

		try {
			Class typeClass = ReflectHelper.classForName( typeName );
			if ( typeClass != null ) {
				return typeFactory.byClass( typeClass, parameters );
			}
		}
		catch ( ClassNotFoundException ignore ) {
		}

		return null;
	}
}
