/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.type.ArrayType;
import org.hibernate.type.BagType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CustomCollectionType;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.IdentifierBagType;
import org.hibernate.type.ListType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.MapType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.OrderedMapType;
import org.hibernate.type.OrderedSetType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedMapType;
import org.hibernate.type.SortedSetType;
import org.hibernate.type.SpecialOneToOneType;
import org.hibernate.type.spi.basic.BasicTypeRegistry;
import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptorRegistry;
import org.hibernate.usertype.ParameterizedType;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Defines a set of available Type instances as isolated from other configurations.  The
 * isolation is defined by each instance of a TypeConfiguration.
 * <p/>
 * Note that each Type is inherently "scoped" to a TypeConfiguration.  We only ever access
 * a Type through a TypeConfiguration - specifically the TypeConfiguration in effect for
 * the current persistence unit.
 * <p/>
 * Even though each Type instance is scoped to a TypeConfiguration, Types do not inherently
 * have access to that TypeConfiguration (mainly because Type is an extension contract - meaning
 * that Hibernate does not manage the full set of Types available in ever TypeConfiguration).
 * However Types will often want access to the TypeConfiguration, which can be achieved by the
 * Type simply implementing the {@link TypeConfigurationAware} interface.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public class TypeConfiguration implements SessionFactoryObserver, TypeDescriptorRegistryAccess {
	private static final CoreMessageLogger log = messageLogger( Scope.class );

	private final Scope scope;

	private final JavaTypeDescriptorRegistry javaTypeDescriptorRegistry;
	private final SqlTypeDescriptorRegistry sqlTypeDescriptorRegistry;
	private final BasicTypeRegistry basicTypeRegistry;

	private boolean initialized = false;

	private final Map<String,EntityPersister> entityPersisterMap = new ConcurrentHashMap<>();
	private final Map<String,CollectionPersister> collectionPersisterMap = new ConcurrentHashMap<>();
	private final Map<String,EmbeddablePersister> embeddablePersisterMap = new ConcurrentHashMap<>();

	public TypeConfiguration() {
		this( new EolScopeMapping() );
	}

	public TypeConfiguration(Mapping mapping) {
		this.scope = new Scope( mapping );
		this.javaTypeDescriptorRegistry = new JavaTypeDescriptorRegistry( this );
		this.sqlTypeDescriptorRegistry = new SqlTypeDescriptorRegistry( this );
		this.basicTypeRegistry = new BasicTypeRegistry( this );

		this.initialized = true;
	}

	/**
	 * Get access to the generic Mapping contract.  This is implemented for both the
	 * boot-time model (Metamodel) and the run-time model (SessionFactory).
	 *
	 * @return The mapping object.  Should almost never return {@code null}.  There is a minor
	 * chance this method would get a {@code null}, but that would be an unsupported use-case.
	 */
	public Mapping getMapping() {
		return scope.getMapping();
	}

	/**
	 * Attempt to resolve the {@link #getMapping()} reference as a SessionFactory (the runtime model).
	 * This will throw an exception if the SessionFactory is not yet bound here.
	 *
	 * @return The SessionFactory
	 *
	 * @throws IllegalStateException if the Mapping reference is not a SessionFactory or the SessionFactory
	 * cannot be resolved; generally either of these cases would mean that the SessionFactory was not yet
	 * bound to this scope object
	 */
	public SessionFactoryImplementor getSessionFactory() {
		return scope.getSessionFactory();
	}

	public MetadataBuildingContext getMetadataBuildingContext() {
		return scope.getMetadataBuildingContext();
	}

	public TypeDescriptorRegistryAccess getTypeDescriptorRegistryAccess() {
		return this;
	}

	public BasicTypeRegistry getBasicTypeRegistry() {
		return basicTypeRegistry;
	}

	public void scope(MetadataBuildingContext metadataBuildingContext) {
		log.debugf( "Scoping TypeConfiguration [%s] to MetadataBuildingContext [%s]", this, metadataBuildingContext );
		scope.setMetadataBuildingContext( metadataBuildingContext );
	}

	public void scope(SessionFactoryImplementor factory) {
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactory [%s]", this, factory );
		scope.setSessionFactory( factory );
		factory.addObserver( this );
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		log.debugf( "Un-scoping TypeConfiguration [%s] to SessionFactory [%s]", this, factory );
		scope.unsetSessionFactory( factory );
	}

	public BasicType resolveCastTargetType(String name) {
		throw new NotYetImplementedException(  );
	}

	public EntityType manyToOne(Class clazz) {
		assert clazz != null;
		return manyToOne( clazz.getName() );
	}

	public EntityType manyToOne(String entityName) {
		throw new NotYetImplementedException(  );
	}

	public EntityType manyToOne(Class clazz, boolean lazy) {
		assert clazz != null;
		return manyToOne( clazz.getName(), lazy );
	}

	public EntityType manyToOne(String entityName, boolean lazy) {
		return manyToOne( entityName, true, null, lazy, true, false, false );
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
				this,
				persistentClass,
				referenceToPrimaryKey,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				ignoreNotFound,
				isLogicalOneToOne
		);
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
				this, persistentClass, foreignKeyType, referenceToPrimaryKey,
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
				this, persistentClass, foreignKeyType, referenceToPrimaryKey,
				uniqueKeyPropertyName, lazy, unwrapProxy, entityName, propertyName
		);
	}

	public Type heuristicType(String typename) {
		throw new NotYetImplementedException(  );
	}

	// collection type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public CollectionType array(String role, String propertyRef, Class elementClass) {
		return new ArrayType( this, role, propertyRef, elementClass );
	}

	public CollectionType list(String role, String propertyRef) {
		return new ListType( this, role, propertyRef );
	}

	public CollectionType bag(String role, String propertyRef) {
		return new BagType( this, role, propertyRef );
	}

	public CollectionType idbag(String role, String propertyRef) {
		return new IdentifierBagType( this, role, propertyRef );
	}

	public CollectionType map(String role, String propertyRef) {
		return new MapType( this, role, propertyRef );
	}

	public CollectionType orderedMap(String role, String propertyRef) {
		return new OrderedMapType( this, role, propertyRef );
	}

	public CollectionType sortedMap(String role, String propertyRef, Comparator comparator) {
		return new SortedMapType( this, role, propertyRef, comparator );
	}

	public CollectionType set(String role, String propertyRef) {
		return new SetType( this, role, propertyRef );
	}

	public CollectionType orderedSet(String role, String propertyRef) {
		return new OrderedSetType( this, role, propertyRef );
	}

	public CollectionType sortedSet(String role, String propertyRef, Comparator comparator) {
		return new SortedSetType( this, role, propertyRef, comparator );
	}

	// component type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public EmbeddedComponentType embeddedComponent(ComponentMetamodel metamodel) {
		return new EmbeddedComponentType( this, metamodel );
	}

	public ComponentType component(ComponentMetamodel metamodel) {
		return new ComponentType( this, metamodel );
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
		CustomCollectionType result = new CustomCollectionType( this, typeClass, role, propertyRef );
		if ( typeParameters != null ) {
			injectParameters( result.getUserType(), typeParameters );
		}
		return result;
	}

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



	/**
	 * Encapsulation of lifecycle concerns for a TypeConfiguration, mainly in regards to
	 * eventually being associated with a SessionFactory.  Goes through the following stages:<ol>
	 *     <li>
	 *         TypeConfiguration initialization - during this phase {@link #getMapping()} will
	 *         return a non-null, no-op impl.  Calls to {@link #getMetadataBuildingContext()} will
	 *         simply return {@code null}, while calls to {@link #getSessionFactory()} will throw
	 *         an exception.
	 *     </li>
	 *     <li>
	 *         Metadata building - during this phase {@link #getMetadataBuildingContext()} will
	 *         return a non-null value and the {@link #getMapping()} return will be the
	 *         {@link MetadataBuildingContext#getMetadataCollector()} reference.  Calls to
	 *         {@link #getSessionFactory()} will throw an exception.
	 *     </li>
	 *     <li>
	 *         live SessionFactory - this is the only phase where calls to {@link #getSessionFactory()}
	 *         are allowed and {@link #getMapping()} returns the SessionFactory itself (since it
	 *         implements that Mapping contract (for now) too.  Calls to {@link #getMetadataBuildingContext()}
	 *         will simply return {@code null}.
	 *     </li>
	 * </ol>
	 */
	private static class Scope {
		private transient Mapping mapping;

		private transient MetadataBuildingContext metadataBuildingContext;

		private String sessionFactoryName;
		private String sessionFactoryUuid;

		Scope(Mapping mapping) {
			this.mapping = mapping;
		}

		public Mapping getMapping() {
			return mapping;
		}

		public MetadataBuildingContext getMetadataBuildingContext() {
			if ( metadataBuildingContext == null ) {
				throw new HibernateException( "TypeConfiguration is not currently scoped to MetadataBuildingContext" );
			}
			return metadataBuildingContext;
		}

		public void setMetadataBuildingContext(MetadataBuildingContext metadataBuildingContext) {
			this.metadataBuildingContext = metadataBuildingContext;
			this.mapping = metadataBuildingContext.getMetadataCollector();
		}

		public SessionFactoryImplementor getSessionFactory() {
			if ( mapping == null ) {
				if ( sessionFactoryName == null && sessionFactoryUuid == null ) {
					throw new HibernateException( "TypeConfiguration was not yet scoped to SessionFactory" );
				}
				mapping = (SessionFactoryImplementor) SessionFactoryRegistry.INSTANCE.findSessionFactory(
						sessionFactoryUuid,
						sessionFactoryName
				);
				if ( mapping == null ) {
					throw new HibernateException(
							"Could not find a SessionFactory [uuid=" + sessionFactoryUuid + ",name=" + sessionFactoryName + "]"
					);
				}
			}

			if ( !SessionFactoryImplementor.class.isInstance( mapping ) ) {
				throw new HibernateException( "TypeConfiguration was not yet scoped to SessionFactory" );
			}

			return (SessionFactoryImplementor) mapping;
		}

		/**
		 * Used by TypeFactory scoping.
		 *
		 * @param factory The SessionFactory that the TypeFactory is being bound to
		 */
		void setSessionFactory(SessionFactoryImplementor factory) {
			if ( this.mapping != null && mapping instanceof SessionFactoryImplementor ) {
				log.scopingTypesToSessionFactoryAfterAlreadyScoped( (SessionFactoryImplementor) mapping, factory );
			}
			else {
				metadataBuildingContext = null;

				sessionFactoryUuid = factory.getUuid();
				String sfName = factory.getSessionFactoryOptions().getSessionFactoryName();
				if ( sfName == null ) {
					final CfgXmlAccessService cfgXmlAccessService = factory.getServiceRegistry()
							.getService( CfgXmlAccessService.class );
					if ( cfgXmlAccessService.getAggregatedConfig() != null ) {
						sfName = cfgXmlAccessService.getAggregatedConfig().getSessionFactoryName();
					}
				}
				sessionFactoryName = sfName;
			}
			this.mapping = factory;
		}

		public void unsetSessionFactory(SessionFactory factory) {
			this.mapping = EolScopeMapping.INSTANCE;
		}
	}

	private static class EolScopeMapping implements Mapping {
		/**
		 * Singleton access
		 */
		public static final EolScopeMapping INSTANCE = new EolScopeMapping();

		@Override
		public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
			throw invalidAccess();
		}

		private RuntimeException invalidAccess() {
			return new IllegalStateException( "Access to this TypeConfiguration is no longer valid" );
		}

		@Override
		public Type getIdentifierType(String className) {
			throw invalidAccess();
		}

		@Override
		public String getIdentifierPropertyName(String className) {
			throw invalidAccess();
		}

		@Override
		public Type getReferencedPropertyType(String className, String propertyName) {
			throw invalidAccess();
		}
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return this;
	}

	@Override
	public JavaTypeDescriptorRegistry getJavaTypeDescriptorRegistry() {
		if ( !initialized ) {
			throw new IllegalStateException( "TypeDescriptorRegistryAccess (TypeConfiguration) initialization incomplete; not yet ready for access" );
		}
		return javaTypeDescriptorRegistry;
	}

	@Override
	public SqlTypeDescriptorRegistry getSqlTypeDescriptorRegistry() {
		if ( !initialized ) {
			throw new IllegalStateException( "TypeDescriptorRegistryAccess (TypeConfiguration) initialization incomplete; not yet ready for access" );
		}
		return sqlTypeDescriptorRegistry;
	}


	@SuppressWarnings("unchecked")
	public <T> EntityPersister<T> findEntityPersister(String entityName) {
		return entityPersisterMap.get( entityName );
	}

	public void register(EntityPersister entityPersister) {
		entityPersisterMap.put( entityPersister.getEntityName(), entityPersister );
	}

	@SuppressWarnings("unchecked")
	public <O,C,E> CollectionPersister<O,C,E> findCollectionPersister(String roleName) {
		return collectionPersisterMap.get( roleName );
	}

	public void register(CollectionPersister collectionPersister) {
		collectionPersisterMap.put( collectionPersister.getRoleName(), collectionPersister );
	}

	@SuppressWarnings("unchecked")
	public <T> EmbeddablePersister<T> findEmbeddableMapper(String roleName) {
		return embeddablePersisterMap.get( roleName );
	}

	public void register(EmbeddablePersister mapper) {
		embeddablePersisterMap.put( mapper.getRoleName(), mapper );
	}

	public Collection<EmbeddablePersister> getEmbeddablePersisters() {
		return embeddablePersisterMap.values();
	}

	public Collection<EntityPersister> getEntityPersisters() {
		return entityPersisterMap.values();
	}

}