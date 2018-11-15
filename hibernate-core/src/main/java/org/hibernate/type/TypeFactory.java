/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.classic.Lifecycle;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
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
 *
 * @deprecated Use {@link TypeConfiguration} instead
 */
@Deprecated
@SuppressWarnings({"unchecked"})
public final class TypeFactory implements Serializable, SessionFactoryObserver {
	private static final CoreMessageLogger LOG = messageLogger( TypeFactory.class );

	/**
	 * @deprecated Use {@link TypeConfiguration}/{@link TypeConfiguration.Scope} instead
	 */
	@Deprecated
	public interface TypeScope extends Serializable {
		TypeConfiguration getTypeConfiguration();
	}

	private final TypeConfiguration typeConfiguration;
	private final TypeScope typeScope;

	private final Map<TypeKey, Type> typeRegistry = new HashMap<>();
	private final Map<TypeKey, CompositeCustomType> compositeTypeRegistry = new HashMap<>();
	private final Map<TypeKey, CustomType> customTypeRegistry = new HashMap<>();
	private final Map<TypeKey, EntityType> entityTypeRegistry = new HashMap<>();
	private final Map<TypeKey, SerializableType> serializableTypeRegistry = new HashMap<>();

	public TypeFactory(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
		this.typeScope = (TypeScope) () -> typeConfiguration;
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		typeRegistry.clear();
		compositeTypeRegistry.clear();
		customTypeRegistry.clear();
		entityTypeRegistry.clear();
		serializableTypeRegistry.clear();
	}

	public Type byClass(Class clazz, Properties parameters) {
		TypeKey typeKey = new TypeKey( clazz, parameters );
		if ( Type.class.isAssignableFrom( clazz ) ) {
			return typeRegistry.computeIfAbsent( typeKey, key -> type( clazz, parameters ) );
		}

		if ( CompositeUserType.class.isAssignableFrom( clazz ) ) {
			return compositeTypeRegistry.computeIfAbsent( typeKey, key -> customComponent( clazz, parameters ) );
		}

		if ( UserType.class.isAssignableFrom( clazz ) ) {
			return customTypeRegistry.computeIfAbsent( typeKey, key -> custom( clazz, parameters ) );
		}

		if ( Lifecycle.class.isAssignableFrom( clazz ) ) {
			// not really a many-to-one association *necessarily*
			return entityTypeRegistry.computeIfAbsent( typeKey, key -> manyToOne( clazz.getName() ) );
		}

		if ( Serializable.class.isAssignableFrom( clazz ) ) {
			return serializableTypeRegistry.computeIfAbsent( typeKey, key -> serializable( clazz ) );
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
		try {
			UserType userType = typeClass.newInstance();
			if ( TypeConfigurationAware.class.isInstance( userType ) ) {
				( (TypeConfigurationAware) userType ).setTypeConfiguration( typeConfiguration );
			}
			injectParameters( userType, parameters );
			return new CustomType( userType );
		}
		catch (Exception e) {
			throw new MappingException( "Unable to instantiate custom type: " + typeClass.getName(), e );
		}
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

	/**
	 * @deprecated Use {@link TypeFactory#oneToOne(String, ForeignKeyDirection, boolean, String, boolean, boolean, String, String, boolean)}
	 *  instead.
	 */
	@Deprecated
	public EntityType oneToOne(
			String persistentClass,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName) {
		return oneToOne( persistentClass, foreignKeyType, referenceToPrimaryKey, uniqueKeyPropertyName, lazy, unwrapProxy, entityName, propertyName, foreignKeyType != ForeignKeyDirection.TO_PARENT );
	}

	/**
	 * @deprecated Use {@link TypeFactory#specialOneToOne(String, ForeignKeyDirection, boolean, String, boolean, boolean, String, String, boolean)}
	 *  instead.
	 */
	@Deprecated
	public EntityType specialOneToOne(
			String persistentClass,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName) {
		return specialOneToOne( persistentClass, foreignKeyType, referenceToPrimaryKey, uniqueKeyPropertyName, lazy, unwrapProxy, entityName, propertyName, foreignKeyType != ForeignKeyDirection.TO_PARENT );
	}

	public EntityType oneToOne(
			String persistentClass,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName,
			boolean constrained) {
		return new OneToOneType(
				typeScope, persistentClass, foreignKeyType, referenceToPrimaryKey,
				uniqueKeyPropertyName, lazy, unwrapProxy, entityName, propertyName, constrained
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
			String propertyName,
			boolean constrained) {
		return new SpecialOneToOneType(
				typeScope, persistentClass, foreignKeyType, referenceToPrimaryKey,
				uniqueKeyPropertyName, lazy, unwrapProxy, entityName, propertyName,
				constrained
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

	/**
	 * @deprecated Use {@link #manyToOne(String, boolean, String, String, boolean, boolean, boolean, boolean)} instead.
	 */
	@Deprecated
	public EntityType manyToOne(
			String persistentClass,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		return manyToOne(
				persistentClass,
				referenceToPrimaryKey,
				uniqueKeyPropertyName,
				null,
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
			String propertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		return new ManyToOneType(
				typeScope,
				persistentClass,
				referenceToPrimaryKey,
				uniqueKeyPropertyName,
				propertyName,
				lazy,
				unwrapProxy,
				ignoreNotFound,
				isLogicalOneToOne
		);
	}

	// collection type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public CollectionType array(String role, String propertyRef, Class elementClass) {
		return new ArrayType( typeScope, role, propertyRef, elementClass );
	}

	public CollectionType list(String role, String propertyRef) {
		return new ListType( typeScope, role, propertyRef );
	}

	public CollectionType bag(String role, String propertyRef) {
		return new BagType( typeScope, role, propertyRef );
	}

	public CollectionType idbag(String role, String propertyRef) {
		return new IdentifierBagType( typeScope, role, propertyRef );
	}

	public CollectionType map(String role, String propertyRef) {
		return new MapType( typeScope, role, propertyRef );
	}

	public CollectionType orderedMap(String role, String propertyRef) {
		return new OrderedMapType( typeScope, role, propertyRef );
	}

	public CollectionType sortedMap(String role, String propertyRef, Comparator comparator) {
		return new SortedMapType( typeScope, role, propertyRef, comparator );
	}

	public CollectionType set(String role, String propertyRef) {
		return new SetType( typeScope, role, propertyRef );
	}

	public CollectionType orderedSet(String role, String propertyRef) {
		return new OrderedSetType( typeScope, role, propertyRef );
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

	private static class TypeKey {
		private final Class clazz;
		private final Properties parameters = new Properties();

		private TypeKey(Class clazz, Properties parameters) {
			this.clazz = clazz;
			if ( parameters != null && !parameters.isEmpty() ) {
				this.parameters.putAll( parameters );
			}
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			TypeKey typeKey = (TypeKey) o;
			return Objects.equals( clazz, typeKey.clazz ) &&
					Objects.equals( parameters, typeKey.parameters );
		}

		@Override
		public int hashCode() {
			return Objects.hash( clazz, parameters );
		}
	}
}
