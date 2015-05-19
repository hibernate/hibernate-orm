/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Properties;

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

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Used internally to build instances of {@link Type}, specifically it builds instances of
 * <p/>
 * <p/>
 * Used internally to obtain instances of <tt>Type</tt>. Applications should use static methods
 * and constants on <tt>org.hibernate.Hibernate</tt>.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings({"unchecked"})
public final class TypeFactory implements Serializable {
	private static final CoreMessageLogger LOG = messageLogger( TypeFactory.class );

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

	// todo : can a Properties be wrapped in unmodifiable in any way?
	private final static Properties EMPTY_PROPERTIES = new Properties();

	public static void injectParameters(Object type, Properties parameters) {
		if ( ParameterizedType.class.isInstance( type ) ) {
			if ( parameters == null ) {
				( (ParameterizedType) type ).setParameterValues( EMPTY_PROPERTIES );
			}
			else {
				( (ParameterizedType) type ).setParameterValues( parameters );
			}
		}
		else if ( parameters != null && !parameters.isEmpty() ) {
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
	@SuppressWarnings({"JavaDoc"})
	public static CompositeCustomType customComponent(
			Class<CompositeUserType> typeClass,
			Properties parameters,
			TypeScope scope) {
		try {
			CompositeUserType userType = typeClass.newInstance();
			injectParameters( userType, parameters );
			return new CompositeCustomType( userType );
		}
		catch (Exception e) {
			throw new MappingException( "Unable to instantiate custom type: " + typeClass.getName(), e );
		}
	}

	/**
	 * @deprecated Use {@link #customCollection(String, java.util.Properties, String, String)}
	 * instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
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
		catch (ClassNotFoundException cnfe) {
			throw new MappingException( "user collection type class not found: " + typeName, cnfe );
		}
		CustomCollectionType result = new CustomCollectionType( typeScope, typeClass, role, propertyRef, embedded );
		if ( typeParameters != null ) {
			injectParameters( result.getUserType(), typeParameters );
		}
		return result;
	}

	public CollectionType customCollection(
			String typeName,
			Properties typeParameters,
			String role,
			String propertyRef) {
		Class typeClass;
		try {
			typeClass = ReflectHelper.classForName( typeName );
		}
		catch (ClassNotFoundException cnfe) {
			throw new MappingException( "user collection type class not found: " + typeName, cnfe );
		}
		CustomCollectionType result = new CustomCollectionType( typeScope, typeClass, role, propertyRef );
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
		catch (Exception e) {
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
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName) {
		return new OneToOneType(
				typeScope, persistentClass, foreignKeyType, referenceToPrimaryKey,
				uniqueKeyPropertyName, lazy, unwrapProxy, entityName, propertyName
		);
	}

	public EntityType specialOneToOne(
			String persistentClass,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName) {
		return new SpecialOneToOneType(
				typeScope, persistentClass, foreignKeyType, referenceToPrimaryKey,
				uniqueKeyPropertyName, lazy, unwrapProxy, entityName, propertyName
		);
	}


	// many-to-one type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public EntityType manyToOne(String persistentClass) {
		return new ManyToOneType( typeScope, persistentClass );
	}

	public EntityType manyToOne(String persistentClass, boolean lazy) {
		return new ManyToOneType( typeScope, persistentClass, lazy );
	}

	/**
	 * @deprecated Use {@link #manyToOne(String, boolean, String, boolean, boolean, boolean, boolean)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public EntityType manyToOne(
			String persistentClass,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean isEmbeddedInXML,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		return manyToOne(
				persistentClass,
				uniqueKeyPropertyName == null,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				ignoreNotFound,
				isLogicalOneToOne
		);
	}

	/**
	 * @deprecated Use {@link #manyToOne(String, boolean, String, boolean, boolean, boolean, boolean)} instead.
	 */
	@Deprecated
	public EntityType manyToOne(
			String persistentClass,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		return manyToOne(
				persistentClass,
				uniqueKeyPropertyName == null,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				ignoreNotFound,
				isLogicalOneToOne
		);
	}

	public EntityType manyToOne(
			String persistentClass,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		return new ManyToOneType(
				typeScope,
				persistentClass,
				referenceToPrimaryKey,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				ignoreNotFound,
				isLogicalOneToOne
		);
	}

	// collection type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated Use {@link #array(String, String, Class)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public CollectionType array(String role, String propertyRef, boolean embedded, Class elementClass) {
		return new ArrayType( typeScope, role, propertyRef, elementClass, embedded );
	}

	public CollectionType array(String role, String propertyRef, Class elementClass) {
		return new ArrayType( typeScope, role, propertyRef, elementClass );
	}

	/**
	 * @deprecated Use {@link #list(String, String)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public CollectionType list(String role, String propertyRef, boolean embedded) {
		return new ListType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType list(String role, String propertyRef) {
		return new ListType( typeScope, role, propertyRef );
	}

	/**
	 * @deprecated Use {@link #bag(String, String)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public CollectionType bag(String role, String propertyRef, boolean embedded) {
		return new BagType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType bag(String role, String propertyRef) {
		return new BagType( typeScope, role, propertyRef );
	}

	/**
	 * @deprecated Use {@link #idbag(String, String)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public CollectionType idbag(String role, String propertyRef, boolean embedded) {
		return new IdentifierBagType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType idbag(String role, String propertyRef) {
		return new IdentifierBagType( typeScope, role, propertyRef );
	}

	/**
	 * @deprecated Use {@link #map(String, String)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public CollectionType map(String role, String propertyRef, boolean embedded) {
		return new MapType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType map(String role, String propertyRef) {
		return new MapType( typeScope, role, propertyRef );
	}

	/**
	 * @deprecated Use {@link #orderedMap(String, String)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public CollectionType orderedMap(String role, String propertyRef, boolean embedded) {
		return new OrderedMapType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType orderedMap(String role, String propertyRef) {
		return new OrderedMapType( typeScope, role, propertyRef );
	}

	/**
	 * @deprecated Use {@link #sortedMap(String, String, java.util.Comparator)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public CollectionType sortedMap(String role, String propertyRef, boolean embedded, Comparator comparator) {
		return new SortedMapType( typeScope, role, propertyRef, comparator, embedded );
	}

	public CollectionType sortedMap(String role, String propertyRef, Comparator comparator) {
		return new SortedMapType( typeScope, role, propertyRef, comparator );
	}

	/**
	 * @deprecated Use {@link #set(String, String)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public CollectionType set(String role, String propertyRef, boolean embedded) {
		return new SetType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType set(String role, String propertyRef) {
		return new SetType( typeScope, role, propertyRef );
	}

	/**
	 * @deprecated Use {@link #orderedSet(String, String)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public CollectionType orderedSet(String role, String propertyRef, boolean embedded) {
		return new OrderedSetType( typeScope, role, propertyRef, embedded );
	}

	public CollectionType orderedSet(String role, String propertyRef) {
		return new OrderedSetType( typeScope, role, propertyRef );
	}

	/**
	 * @deprecated Use {@link #sortedSet(String, String, java.util.Comparator)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public CollectionType sortedSet(String role, String propertyRef, boolean embedded, Comparator comparator) {
		return new SortedSetType( typeScope, role, propertyRef, comparator, embedded );
	}

	public CollectionType sortedSet(String role, String propertyRef, Comparator comparator) {
		return new SortedSetType( typeScope, role, propertyRef, comparator );
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
		return new AnyType( typeScope, metaType, identifierType );
	}
}
