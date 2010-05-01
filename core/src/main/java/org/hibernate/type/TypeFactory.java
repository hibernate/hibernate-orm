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
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.tuple.StandardProperty;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.hibernate.util.ReflectHelper;

/**
 * Used internally to obtain instances of <tt>Type</tt>. Applications should use static methods
 * and constants on <tt>org.hibernate.Hibernate</tt>.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings({ "unchecked" })
public final class TypeFactory implements Serializable {
	private static final Logger log = LoggerFactory.getLogger( TypeFactory.class );

	private final TypeScopeImpl typeScope = new TypeScopeImpl();

	public static interface TypeScope extends Serializable {
		public SessionFactoryImplementor resolveFactory();
	}

	private static class TypeScopeImpl implements TypeFactory.TypeScope {
		private SessionFactoryImplementor factory;

		public void injectSessionFactory(SessionFactoryImplementor factory) {
			if ( factory != null ) {
				log.warn( "Scoping types to session factory7 after already scoped" );
			}
			this.factory = factory;
		}

		public SessionFactoryImplementor resolveFactory() {
			if ( factory == null ) {
				throw new HibernateException( "SessionFactory for type scoping not yet known" );
			}
			return factory;
		}
	}

	public void injectSessionFactory(SessionFactoryImplementor factory) {
		typeScope.injectSessionFactory( factory );
	}

	public Type byClass(Class clazz, Properties parameters) {
		if ( Type.class.isAssignableFrom( clazz ) ) {
			return type( (Class<Type>) clazz, parameters );
		}

		if ( CompositeUserType.class.isAssignableFrom( clazz ) ) {
			return customComponent( (Class<CompositeUserType>) clazz, parameters );
		}

		if ( UserType.class.isAssignableFrom( clazz ) ) {
			return custom( (Class<UserType>) clazz, parameters );
		}

		if ( Lifecycle.class.isAssignableFrom( clazz ) ) {
			// not really a many-to-one association *necessarily*
			return new ManyToOneType( clazz.getName() );
		}

		if ( Serializable.class.isAssignableFrom( clazz ) ) {
			return serializable( clazz );
		}

		return null;
	}

	public Type type(Class<Type> typeClass, Properties parameters) {
		try {
			Type type = typeClass.newInstance();
			injectParameters( type, parameters );
			return type;
		}
		catch (Exception e) {
			throw new MappingException( "Could not instantiate Type: " + typeClass.getName(), e );
		}
	}

	public static void injectParameters(Object type, Properties parameters) {
		if ( ParameterizedType.class.isInstance( type ) ) {
			( (ParameterizedType) type ).setParameterValues(parameters);
		}
		else if ( parameters!=null && !parameters.isEmpty() ) {
			throw new MappingException( "type is not parameterized: " + type.getClass().getName() );
		}
	}

	public static CompositeCustomType customComponent(Class<CompositeUserType> typeClass, Properties parameters) {
		try {
			CompositeUserType userType = typeClass.newInstance();
			injectParameters( userType, parameters );
			return new CompositeCustomType( userType );
		}
		catch ( Exception e ) {
			throw new MappingException( "Unable to instantiate custom type: " + typeClass.getName(), e );
		}
	}

	public static CustomType custom(Class<UserType> typeClass, Properties parameters) {
		try {
			UserType userType = typeClass.newInstance();
			injectParameters( userType, parameters );
			return new CustomType( userType );
		}
		catch ( Exception e ) {
			throw new MappingException( "Unable to instantiate custom type: " + typeClass.getName(), e );
		}
	}

	/**
	 * Build a {@link SerializableType} from the given {@link Serializable} class.
	 *
	 * @param serializableClass The {@link Serializable} class.
	 * @param <T> The actual class type (extends Serializable)
	 *
	 * @return The built {@link SerializableType}
	 */
	public static <T extends Serializable> SerializableType<T> serializable(Class<T> serializableClass) {
		return new SerializableType<T>( serializableClass );
	}


	/**
	 * A one-to-one association type for the given class
	 */
	public static EntityType oneToOne(
			String persistentClass,
			ForeignKeyDirection foreignKeyType,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean isEmbeddedInXML,
			String entityName,
			String propertyName
	) {
		return new OneToOneType(
				persistentClass,
				foreignKeyType,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				isEmbeddedInXML,
				entityName,
				propertyName
			);
	}

	/**
	 * A many-to-one association type for the given class
	 */
	public static EntityType manyToOne(String persistentClass) {
		return new ManyToOneType( persistentClass );
	}

	/**
	 * A many-to-one association type for the given class
	 */
	public static EntityType manyToOne(String persistentClass, boolean lazy) {
		return new ManyToOneType( persistentClass, lazy );
	}

	/**
	 * A many-to-one association type for the given class
	 *
	 * @deprecated Use {@link #manyToOne(String, String, boolean, boolean, boolean, boolean, boolean)}
	 */
	public static EntityType manyToOne(
			String persistentClass,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean isEmbeddedInXML,
			boolean ignoreNotFound) {
		//noinspection deprecation
		return new ManyToOneType(
				persistentClass,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				isEmbeddedInXML,
				ignoreNotFound
		);
	}

	/**
	 * A many-to-one association type for the given class
	 */
	public static EntityType manyToOne(
			String persistentClass,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean isEmbeddedInXML,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		return new ManyToOneType(
				persistentClass,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				isEmbeddedInXML,
				ignoreNotFound,
				isLogicalOneToOne
		);
	}

	/**
	 * The legacy contract.
	 *
	 * @deprecated Use {@link #customCollection(String, java.util.Properties, String, String, boolean)} instead
	 */
	public static CollectionType customCollection(
			String typeName,
			String role,
			String propertyRef,
			boolean embedded) {
		return customCollection( typeName, null, role, propertyRef, embedded );
	}

	public static CollectionType customCollection(
			String typeName,
			Properties typeParameters,
			String role,
			String propertyRef,
			boolean embedded) {
		Class typeClass;
		try {
			typeClass = ReflectHelper.classForName( typeName );
		}
		catch ( ClassNotFoundException cnfe ) {
			throw new MappingException( "user collection type class not found: " + typeName, cnfe );
		}
		CustomCollectionType result = new CustomCollectionType( typeClass, role, propertyRef, embedded );
		if ( typeParameters != null ) {
			TypeFactory.injectParameters( result.getUserType(), typeParameters );
		}
		return result;
	}

	// Collection Types:

	public static CollectionType array(String role, String propertyRef, boolean embedded,
			Class elementClass) {
		return new ArrayType( role, propertyRef, elementClass, embedded );
	}

	public static CollectionType list(String role, String propertyRef, boolean embedded) {
		return new ListType( role, propertyRef, embedded );
	}

	public static CollectionType bag(String role, String propertyRef, boolean embedded) {
		return new BagType( role, propertyRef, embedded );
	}

	public static CollectionType idbag(String role, String propertyRef, boolean embedded) {
		return new IdentifierBagType( role, propertyRef, embedded );
	}

	public static CollectionType map(String role, String propertyRef, boolean embedded) {
		return new MapType( role, propertyRef, embedded );
	}

	public static CollectionType orderedMap(String role, String propertyRef, boolean embedded) {
		return new OrderedMapType( role, propertyRef, embedded );
	}

	public static CollectionType set(String role, String propertyRef, boolean embedded) {
		return new SetType( role, propertyRef, embedded );
	}

	public static CollectionType orderedSet(String role, String propertyRef, boolean embedded) {
		return new OrderedSetType( role, propertyRef, embedded );
	}

	public static CollectionType sortedMap(String role, String propertyRef, boolean embedded,
			Comparator comparator) {
		return new SortedMapType( role, propertyRef, comparator, embedded );
	}

	public static CollectionType sortedSet(String role, String propertyRef, boolean embedded,
			Comparator comparator) {
		return new SortedSetType( role, propertyRef, comparator, embedded );
	}


	// convenience methods relating to operations across arrays of types ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Deep copy a series of values from one array to another...
	 *
	 * @param values The values to copy (the source)
	 * @param types The value types
	 * @param copy an array indicating which values to include in the copy
	 * @param target The array into which to copy the values
	 * @param session The originating session
	 *
	 * @deprecated Use {@link TypeHelper#deepCopy} instead
	 */
	public static void deepCopy(
			final Object[] values,
			final Type[] types,
			final boolean[] copy,
			final Object[] target,
			final SessionImplementor session) {
		TypeHelper.deepCopy( values, types, copy, target, session );
	}

	/**
	 * Apply the {@link Type#beforeAssemble} operation across a series of values.
	 *
	 * @param row The values
	 * @param types The value types
	 * @param session The originating session
	 *
	 * @deprecated Use {@link TypeHelper#beforeAssemble} instead
	 */
	public static void beforeAssemble(
			final Serializable[] row,
			final Type[] types,
			final SessionImplementor session) {
		TypeHelper.beforeAssemble( row, types, session );
	}

	/**
	 * Apply the {@link Type#assemble} operation across a series of values.
	 *
	 * @param row The values
	 * @param types The value types
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 *
	 * @return The assembled state
	 *
	 * @deprecated Use {@link TypeHelper#assemble} instead
	 */
	public static Object[] assemble(
			final Serializable[] row,
			final Type[] types,
			final SessionImplementor session,
			final Object owner) {
		return TypeHelper.assemble( row, types, session, owner );
	}

	/**
	 * Apply the {@link Type#disassemble} operation across a series of values.
	 *
	 * @param row The values
	 * @param types The value types
	 * @param nonCacheable An array indicating which values to include in the disassembled state
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 *
	 * @return The disassembled state
	 *
	 * @deprecated Use {@link TypeHelper#disassemble} instead
	 */
	public static Serializable[] disassemble(
			final Object[] row,
			final Type[] types,
			final boolean[] nonCacheable,
			final SessionImplementor session,
			final Object owner) {
		return TypeHelper.disassemble( row, types, nonCacheable, session, owner );
	}

	/**
	 * Apply the {@link Type#replace} operation across a series of values.
	 *
	 * @param original The source of the state
	 * @param target The target into which to replace the source values.
	 * @param types The value types
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 * @param copyCache A map representing a cache of already replaced state
	 *
	 * @return The replaced state
	 *
	 * @deprecated Use {@link TypeHelper#replace} instead
	 */
	public static Object[] replace(
			final Object[] original,
			final Object[] target,
			final Type[] types,
			final SessionImplementor session,
			final Object owner,
			final Map copyCache) {
		return TypeHelper.replace( original, target, types, session, owner, copyCache );
	}

	/**
	 * Apply the {@link Type#replace} operation across a series of values.
	 *
	 * @param original The source of the state
	 * @param target The target into which to replace the source values.
	 * @param types The value types
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 * @param copyCache A map representing a cache of already replaced state
	 * @param foreignKeyDirection FK directionality to be applied to the replacement
	 *
	 * @return The replaced state
	 *
	 * @deprecated Use {@link TypeHelper#replace} instead
	 */
	public static Object[] replace(
			final Object[] original,
			final Object[] target,
			final Type[] types,
			final SessionImplementor session,
			final Object owner,
			final Map copyCache,
			final ForeignKeyDirection foreignKeyDirection) {
		return TypeHelper.replace( original, target, types, session, owner, copyCache, foreignKeyDirection );
	}

	/**
	 * Apply the {@link Type#replace} operation across a series of values, as
	 * long as the corresponding {@link Type} is an association.
	 * <p/>
	 * If the corresponding type is a component type, then apply {@link Type#replace}
	 * across the component subtypes but do not replace the component value itself.
	 *
	 * @param original The source of the state
	 * @param target The target into which to replace the source values.
	 * @param types The value types
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 * @param copyCache A map representing a cache of already replaced state
	 * @param foreignKeyDirection FK directionality to be applied to the replacement
	 *
	 * @return The replaced state
	 *
	 * @deprecated Use {@link TypeHelper#replaceAssociations} instead
	 */
	public static Object[] replaceAssociations(
			final Object[] original,
			final Object[] target,
			final Type[] types,
			final SessionImplementor session,
			final Object owner,
			final Map copyCache,
			final ForeignKeyDirection foreignKeyDirection) {
		return TypeHelper.replaceAssociations( original, target, types, session, owner, copyCache, foreignKeyDirection );
	}

	/**
	 * Determine if any of the given field values are dirty, returning an array containing
	 * indices of the dirty fields.
	 * <p/>
	 * If it is determined that no fields are dirty, null is returned.
	 *
	 * @param properties The property definitions
	 * @param currentState The current state of the entity
	 * @param previousState The baseline state of the entity
	 * @param includeColumns Columns to be included in the dirty checking, per property
	 * @param anyUninitializedProperties Does the entity currently hold any uninitialized property values?
	 * @param session The session from which the dirty check request originated.
	 *
	 * @return Array containing indices of the dirty properties, or null if no properties considered dirty.
	 *
	 * @deprecated Use {@link TypeHelper#findDirty} instead
	 */
	public static int[] findDirty(
			final StandardProperty[] properties,
			final Object[] currentState,
			final Object[] previousState,
			final boolean[][] includeColumns,
			final boolean anyUninitializedProperties,
			final SessionImplementor session) {
		return TypeHelper.findDirty( properties, currentState, previousState,
				includeColumns, anyUninitializedProperties, session );
	}

	/**
	 * Determine if any of the given field values are modified, returning an array containing
	 * indices of the modified fields.
	 * <p/>
	 * If it is determined that no fields are dirty, null is returned.
	 *
	 * @param properties The property definitions
	 * @param currentState The current state of the entity
	 * @param previousState The baseline state of the entity
	 * @param includeColumns Columns to be included in the mod checking, per property
	 * @param anyUninitializedProperties Does the entity currently hold any uninitialized property values?
	 * @param session The session from which the dirty check request originated.
	 *
	 * @return Array containing indices of the modified properties, or null if no properties considered modified.
	 *
	 * @deprecated Use {@link TypeHelper#findModified} instead
	 */
	public static int[] findModified(
			final StandardProperty[] properties, 
			final Object[] currentState,
			final Object[] previousState,
			final boolean[][] includeColumns,
			final boolean anyUninitializedProperties,
			final SessionImplementor session) {
		return TypeHelper.findModified( properties, currentState, previousState,
				includeColumns, anyUninitializedProperties, session );
	}

}
