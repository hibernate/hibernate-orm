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
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * Used internally to build instances of {@link Type}, specifically it builds instances of
 *
 *
 * Used internally to obtain instances of <tt>Type</tt>. Applications should use static methods
 * and constants on <tt>org.hibernate.Hibernate</tt>.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings({ "unchecked" })
public final class TypeFactory implements Serializable {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, TypeFactory.class.getName());

	private final TypeScopeImpl typeScope = new TypeScopeImpl();

	public static interface TypeScope extends Serializable {
		public SessionFactoryImplementor resolveFactory();
	}

	private static class TypeScopeImpl implements TypeFactory.TypeScope {
		private SessionFactoryImplementor factory;

		public void injectSessionFactory(SessionFactoryImplementor factory) {
			if ( this.factory != null ) {
				LOG.scopingTypesToSessionFactoryAfterAlreadyScoped( this.factory, factory );
			}
			else {
				LOG.tracev( "Scoping types to session factory {0}", factory );
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

	public SessionFactoryImplementor resolveSessionFactory() {
		return typeScope.resolveFactory();
	}

	public Type byClass(Class clazz, Properties parameters) {
		if ( Type.class.isAssignableFrom( clazz ) ) {
			return type( clazz, parameters );
		}

		if ( CompositeUserType.class.isAssignableFrom( clazz ) ) {
			return customComponent( clazz, parameters );
		}

		if ( UserType.class.isAssignableFrom( clazz ) ) {
			return custom( clazz, parameters );
		}

		if ( Lifecycle.class.isAssignableFrom( clazz ) ) {
			// not really a many-to-one association *necessarily*
			return manyToOne( clazz.getName() );
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

	public CompositeCustomType customComponent(Class<CompositeUserType> typeClass, Properties parameters) {
		return customComponent( typeClass, parameters, typeScope );
	}

	/**
	 * @deprecated Only for use temporary use by {@link org.hibernate.Hibernate}
	 */
	@Deprecated
    @SuppressWarnings({ "JavaDoc" })
	public static CompositeCustomType customComponent(Class<CompositeUserType> typeClass, Properties parameters, TypeScope scope) {
		try {
			CompositeUserType userType = typeClass.newInstance();
			injectParameters( userType, parameters );
			return new CompositeCustomType( userType );
		}
		catch ( Exception e ) {
			throw new MappingException( "Unable to instantiate custom type: " + typeClass.getName(), e );
		}
	}

	public CollectionType customCollection(
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
		CustomCollectionType result = new CustomCollectionType( typeScope, typeClass, role, propertyRef, embedded );
		if ( typeParameters != null ) {
			injectParameters( result.getUserType(), typeParameters );
		}
		return result;
	}

	public CustomType custom(Class<UserType> typeClass, Properties parameters) {
		return custom( typeClass, parameters, typeScope );
	}

	/**
	 * @deprecated Only for use temporary use by {@link org.hibernate.Hibernate}
	 */
	@Deprecated
    public static CustomType custom(Class<UserType> typeClass, Properties parameters, TypeScope scope) {
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


	// one-to-one type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public EntityType oneToOne(
			String persistentClass,
			ForeignKeyDirection foreignKeyType,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean isEmbeddedInXML,
			String entityName,
			String propertyName) {
		return new OneToOneType( typeScope, persistentClass, foreignKeyType, uniqueKeyPropertyName,
				lazy, unwrapProxy, isEmbeddedInXML, entityName, propertyName );
	}

	public EntityType specialOneToOne(
			String persistentClass,
			ForeignKeyDirection foreignKeyType,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName) {
		return new SpecialOneToOneType( typeScope, persistentClass, foreignKeyType, uniqueKeyPropertyName,
				lazy, unwrapProxy, entityName, propertyName );
	}


	// many-to-one type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public EntityType manyToOne(String persistentClass) {
		return new ManyToOneType( typeScope, persistentClass );
	}

	public EntityType manyToOne(String persistentClass, boolean lazy) {
		return new ManyToOneType( typeScope, persistentClass, lazy );
	}

	public EntityType manyToOne(
			String persistentClass,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean isEmbeddedInXML,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		return new ManyToOneType(
				typeScope,
				persistentClass,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				isEmbeddedInXML,
				ignoreNotFound,
				isLogicalOneToOne
		);
	}


	// collection type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public CollectionType array(String role, String propertyRef, boolean embedded, Class elementClass) {
		return new ArrayType( typeScope, role, propertyRef, elementClass, embedded );
	}

	public CollectionType list(String role, String propertyRef, boolean embedded) {
		return new ListType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType bag(String role, String propertyRef, boolean embedded) {
		return new BagType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType idbag(String role, String propertyRef, boolean embedded) {
		return new IdentifierBagType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType map(String role, String propertyRef, boolean embedded) {
		return new MapType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType orderedMap(String role, String propertyRef, boolean embedded) {
		return new OrderedMapType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType sortedMap(String role, String propertyRef, boolean embedded, Comparator comparator) {
		return new SortedMapType( typeScope, role, propertyRef, comparator, embedded );
	}

	public CollectionType set(String role, String propertyRef, boolean embedded) {
		return new SetType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType orderedSet(String role, String propertyRef, boolean embedded) {
		return new OrderedSetType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType sortedSet(String role, String propertyRef, boolean embedded, Comparator comparator) {
		return new SortedSetType( typeScope, role, propertyRef, comparator, embedded );
	}


	// component type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public ComponentType component(ComponentMetamodel metamodel) {
		return new ComponentType( typeScope, metamodel );
	}

	public EmbeddedComponentType embeddedComponent(ComponentMetamodel metamodel) {
		return new EmbeddedComponentType( typeScope, metamodel );
	}


	// any type builder ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Type any(Type metaType, Type identifierType) {
		return new AnyType( metaType, identifierType );
	}
}
