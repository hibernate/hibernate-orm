/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityGraph;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.EntityGraphImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationProcess;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.sqm.tree.expression.BinaryArithmeticSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralSqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.PolymorphicEntityValuedExpressableType;
import org.hibernate.sql.ast.produce.sqm.internal.PolymorphicEntityValuedExpressableTypeImpl;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.java.spi.MappedSuperclassJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry;

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
public class TypeConfiguration implements SessionFactoryObserver {
	private static final CoreMessageLogger log = messageLogger( Scope.class );

	// todo : (
	private final Scope scope;
	private boolean initialized = false;

	// things available during both boot and runtime ("active") lifecycle phases
	private final JavaTypeDescriptorRegistry javaTypeDescriptorRegistry;
	private final SqlTypeDescriptorRegistry sqlTypeDescriptorRegistry;
	private final BasicTypeRegistry basicTypeRegistry;

	private final Map<String,EntityDescriptor<?>> entityPersisterMap = new ConcurrentHashMap<>();
	private final Set<EntityHierarchy> entityHierarchies = ConcurrentHashMap.newKeySet();
	private final Map<String,PersistentCollectionDescriptor<?,?,?>> collectionPersisterMap = new ConcurrentHashMap<>();
	private final Map<String,EmbeddedTypeDescriptor<?>> embeddablePersisterMap = new ConcurrentHashMap<>();

	private final Map<String,String> importMap = new ConcurrentHashMap<>();
	private final Set<EntityNameResolver> entityNameResolvers = ConcurrentHashMap.newKeySet();

	private final Map<JavaTypeDescriptor, PolymorphicEntityValuedExpressableType<?>> polymorphicEntityReferenceMap = new HashMap<>();
	private final Map<JavaTypeDescriptor,String> entityProxyInterfaceMap = new ConcurrentHashMap<>();
	private final Map<String,Set<String>> collectionRolesByEntityParticipant = new ConcurrentHashMap<>();
	private final Map<EmbeddableJavaDescriptor<?>,Set<String>> embeddedRolesByEmbeddableType = new ConcurrentHashMap<>();

	private final Map<String,EntityGraph> entityGraphMap = new ConcurrentHashMap<>();

	// todo (6.0) : I believe that Mapping can go away.  In all respects TypeConfiguration is meant as a replacement for the concept Mapping is meant to solve

	public TypeConfiguration(BootstrapContext bootstrapContext) {
		this();
		StandardBasicTypes.prime( this );
	}

	public TypeConfiguration() {
		this.scope = new Scope();
		this.javaTypeDescriptorRegistry = new JavaTypeDescriptorRegistry( this );
		this.sqlTypeDescriptorRegistry = new SqlTypeDescriptorRegistry( this );
		this.basicTypeRegistry = new BasicTypeRegistry( this );

		this.initialized = true;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Access to registries

	public BasicTypeRegistry getBasicTypeRegistry() {
		return basicTypeRegistry;
	}

	public JavaTypeDescriptorRegistry getJavaTypeDescriptorRegistry() {
		if ( !initialized ) {
			throw new IllegalStateException( "TypeConfiguration initialization incomplete; not yet ready for access" );
		}
		return javaTypeDescriptorRegistry;
	}

	public SqlTypeDescriptorRegistry getSqlTypeDescriptorRegistry() {
		if ( !initialized ) {
			throw new IllegalStateException( "TypeConfiguration initialization incomplete; not yet ready for access" );
		}
		return sqlTypeDescriptorRegistry;
	}

	public Set<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers;
	}

	public Map<String, String> getImportMap() {
		return Collections.unmodifiableMap( importMap );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named EntityGraph map access

	public Map<String, EntityGraph> getEntityGraphMap() {
		return Collections.unmodifiableMap( entityGraphMap );
	}

	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		if ( entityGraph instanceof EntityGraphImplementor ) {
			entityGraph = ( (EntityGraphImplementor<T>) entityGraph ).makeImmutableCopy( graphName );
		}

		final EntityGraph old = entityGraphMap.put( graphName, entityGraph );
		if ( old != null ) {
			log.debugf( "EntityGraph being replaced on EntityManagerFactory for name %s", graphName );
		}
	}

	public <T> EntityGraph<T> findEntityGraphByName(String name) {
		return entityGraphMap.get( name );
	}

	public <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
		final EntityDescriptor<? extends T> entityPersister = findEntityDescriptor( entityClass );
		if ( entityPersister == null ) {
			throw new IllegalArgumentException( "Given class is not an entity : " + entityClass.getName() );
		}

		final List<EntityGraph<? super T>> results = new ArrayList<>();

		for ( EntityGraph entityGraph : entityGraphMap.values() ) {
			if ( !EntityGraphImplementor.class.isInstance( entityGraph ) ) {
				continue;
			}

			final EntityGraphImplementor egi = (EntityGraphImplementor) entityGraph;
			if ( egi.appliesTo( entityPersister ) ) {
				results.add( egi );
			}
		}

		return results;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityPersister access

	/**
	 * Retrieve all EntityPersisters, keyed by entity-name
	 */
	public Map<String,EntityDescriptor<?>> getEntityPersisterMap() {
		return Collections.unmodifiableMap( entityPersisterMap );
	}

	/**
	 * Retrieve all EntityPersisters
	 */
	public Collection<EntityDescriptor<?>> getEntityDescriptors() {
		return entityPersisterMap.values();
	}

	/**
	 * Retrieve an EntityPersister by entity-name.  Returns {@code null} if not known.
	 */
	@SuppressWarnings("unchecked")
	public <T> EntityDescriptor<T> findEntityDescriptor(String entityName) {
		if ( importMap.containsKey( entityName ) ) {
			entityName = importMap.get( entityName );
		}
		return (EntityDescriptor<T>) entityPersisterMap.get( entityName );
	}

	/**
	 * Retrieve an EntityPersister by entity-name.  Throws exception if not known.
	 */
	public <T> EntityDescriptor<T> resolveEntityDescriptor(String entityName) throws UnknownEntityTypeException {
		final EntityDescriptor<T> resolved = findEntityDescriptor( entityName );
		if ( resolved != null ) {
			return resolved;
		}

		throw new UnknownEntityTypeException( "Could not resolve EntityPersister by entity name [" + entityName + "]" );
	}

	public <T> EntityDescriptor<? extends T> findEntityDescriptor(Class<T> javaType) {
		EntityDescriptor<? extends T> entityPersister = findEntityDescriptor( javaType.getName() );
		if ( entityPersister == null ) {
			JavaTypeDescriptor javaTypeDescriptor = getJavaTypeDescriptorRegistry().getDescriptor( javaType );
			if ( javaTypeDescriptor != null && javaTypeDescriptor instanceof MappedSuperclassJavaDescriptor ) {
				String mappedEntityName = entityProxyInterfaceMap.get( javaTypeDescriptor );
				if ( mappedEntityName != null ) {
					entityPersister = findEntityDescriptor( mappedEntityName );
				}
			}
		}

		return entityPersister;
	}

	public <T> EntityDescriptor<? extends T> resolveEntityDescriptor(Class<T> javaType) {
		final EntityDescriptor<? extends T> entityPersister = findEntityDescriptor( javaType );
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Could not resolve EntityPersister by Java type [" + javaType.getName() + "]" );
		}
		return entityPersister;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CollectionPersister access

	/**
	 * Retrieve all CollectionPersisters, keyed by role (path)
	 */
	public Map<String,PersistentCollectionDescriptor<?,?,?>> getCollectionPersisterMap() {
		return Collections.unmodifiableMap( collectionPersisterMap );
	}

	/**
	 * Retrieve all CollectionPersisters
	 */
	public Collection<PersistentCollectionDescriptor<?,?,?>> getCollectionPersisters() {
		return collectionPersisterMap.values();
	}

	/**
	 * Locate a CollectionPersister by role (path).  Returns {@code null} if not known
	 */
	@SuppressWarnings("unchecked")
	public <O,C,E> PersistentCollectionDescriptor<O,C,E> findCollectionPersister(String roleName) {
		return (PersistentCollectionDescriptor<O, C, E>) collectionPersisterMap.get( roleName );
	}

	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return collectionRolesByEntityParticipant.get( entityName );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddablePersister access

	public Collection<EmbeddedTypeDescriptor<?>> getEmbeddablePersisters() {
		return embeddablePersisterMap.values();
	}

	@SuppressWarnings("unchecked")
	public <T> EmbeddedTypeDescriptor<T> findEmbeddablePersister(String roleName) {
		return (EmbeddedTypeDescriptor<T>) embeddablePersisterMap.get( roleName );
	}

	public <T> EmbeddedTypeDescriptor<T> findEmbeddablePersister(Class<T> javaType) {
		final JavaTypeDescriptor javaTypeDescriptor = getJavaTypeDescriptorRegistry().getDescriptor( javaType );
		if ( javaType == null || !EmbeddableJavaDescriptor.class.isInstance( javaTypeDescriptor ) ) {
			return null;
		}

		final Set<String> roles = embeddedRolesByEmbeddableType.get( javaTypeDescriptor );
		if ( roles == null || roles.isEmpty() || roles.size() > 1 ) {
			return null;
		}

		return findEmbeddablePersister( roles.iterator().next() );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQM Query handling
	//		- everything within this "block" of methods relates to SQM
	// 			interpretation of queries and implements its calls accordingly

	@SuppressWarnings("unchecked")
	public <T> EntityValuedExpressableType<T> resolveEntityReference(String entityName) {
		if ( importMap.containsKey( entityName ) ) {
			entityName = importMap.get( entityName );
		}

		final EntityDescriptor namedPersister = findEntityDescriptor( entityName );
		if ( namedPersister != null ) {
			return namedPersister;
		}

		final Class requestedClass = resolveRequestedClass( entityName );
		if ( requestedClass != null ) {
			return resolveEntityReference( requestedClass );
		}

		throw new IllegalArgumentException( "Per JPA spec : no entity named " + entityName );
	}

	private Class resolveRequestedClass(String entityName) {
		try {
			return getSessionFactory().getServiceRegistry().getService( ClassLoaderService.class ).classForName( entityName );
		}
		catch (ClassLoadingException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> EntityValuedExpressableType<T> resolveEntityReference(Class<T> javaType) {
		// see if we know of this Class by name as an EntityPersister key
		if ( getEntityPersisterMap().containsKey( javaType.getName() ) ) {
			// and if so, return that persister
			return (EntityValuedExpressableType<T>) getEntityPersisterMap().get( javaType.getName() );
		}

		final JavaTypeDescriptor<T> jtd = getJavaTypeDescriptorRegistry().getDescriptor( javaType );
		if ( jtd == null ) {
			throw new HibernateException( "Could not locate JavaTypeDescriptor : " + javaType.getName() );
		}

		// next check entityProxyInterfaceMap
		final String proxyEntityName = entityProxyInterfaceMap.get( jtd );
		if ( proxyEntityName != null ) {
			return (EntityValuedExpressableType<T>) getEntityPersisterMap().get( proxyEntityName );
		}

		// otherwise, trye to handle it as a polymorphic reference
		if ( polymorphicEntityReferenceMap.containsKey( jtd ) ) {
			return (EntityValuedExpressableType<T>) polymorphicEntityReferenceMap.get( jtd );
		}

		final Set<EntityDescriptor<?>> implementors = getImplementors( javaType );
		if ( !implementors.isEmpty() ) {
			final PolymorphicEntityValuedExpressableTypeImpl entityReference = new PolymorphicEntityValuedExpressableTypeImpl(
					(EntityJavaDescriptor) jtd,
					implementors
			);
			polymorphicEntityReferenceMap.put( jtd, entityReference );
			return entityReference;
		}

		throw new IllegalArgumentException( "Could not resolve entity reference : " + javaType.getName() );
	}

	@SuppressWarnings("unchecked")
	public Set<EntityDescriptor<?>> getImplementors(Class javaType) {
		// if the javaType refers directly to an EntityPersister by Class name, return just it.
		final EntityDescriptor<?> exactMatch = getEntityPersisterMap().get( javaType.getName() );
		if ( exactMatch != null ) {
			return Collections.singleton( exactMatch );
		}

		final HashSet<EntityDescriptor<?>> matchingPersisters = new HashSet<>();

		for ( EntityDescriptor entityPersister : getEntityPersisterMap().values() ) {
			if ( entityPersister.getJavaType() == null ) {
				continue;
			}

			// todo : explicit/implicit polymorphism...
			// todo : handle "duplicates" within a hierarchy
			// todo : in fact we may want to cycle through persisters via entityHierarchies and walking the subclass graph rather than walking each persister linearly (in random order)

			if ( javaType.isAssignableFrom( entityPersister.getJavaType() ) ) {
				matchingPersisters.add( entityPersister );
			}
		}

		return matchingPersisters;
	}



















	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Scoping


	/**
	 * Get access to the generic Mapping contract.  This is implemented for both the
	 * boot-time model (Metamodel) and the run-time model (SessionFactory).
	 *
	 * @return The mapping object.  Should almost never return {@code null}.  There is a minor
	 * chance this method would get a {@code null}, but that would be an unsupported use-case.
	 */
// todo (6.0) : as discussed below in Scope, it would be great to have a common contract exposable here
//	public Mapping getMapping() {
//		return scope.getMapping();
//	}

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

	public void scope(MetadataBuildingContext metadataBuildingContext) {
		log.debugf( "Scoping TypeConfiguration [%s] to MetadataBuildingContext [%s]", this, metadataBuildingContext );
		scope.setMetadataBuildingContext( metadataBuildingContext );

		for ( Map.Entry<String, String> importEntry : metadataBuildingContext.getMetadataCollector().getImports().entrySet() ) {
			if ( importMap.containsKey( importEntry.getKey() ) ) {
				continue;
			}

			importMap.put( importEntry.getKey(), importEntry.getValue() );
		}
	}

	public void scope(SessionFactoryImplementor factory) {
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactory [%s]", this, factory );

		scope.setSessionFactory( factory );
		factory.addObserver( this );

		new RuntimeModelCreationProcess( factory, getMetadataBuildingContext() ).execute();
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		log.tracef( "Handling #sessionFactoryClosed from [%s] for TypeConfiguration" );
		scope.unsetSessionFactory( factory );

		// todo : come back and implement this...
		//		release Database, persister Maps, etc... things that are only
		// 		valid while the TypeConfiguration is scoped to SessionFactory
		throw new NotYetImplementedException(  );
	}

	// todo (6.0) - have this algorithm be extendable by users.
	// 		I have received at least one user request for this, and I can completely see the
	// 		benefit of this as they described it.  Basically consider a query containing
	// 		`p.x + p.y`.  If `y` is a standard integer type, but `x` is a custom (user) integral
	// 		type, then what is the type of the arithmetic expression?  From the HipChat discussion:
	//
	//		[8:18 AM] Steve Ebersole: btw... what got me started thinking about this is thinking of ways to allow custom hooks into the types of literals recognized (and how) and the types of validation checks we do
	//		[8:18 AM] Steve Ebersole: allowing custom literal types becomes easy(er) if we follow the escape-like syntax
	//		[8:19 AM] Steve Ebersole: {[something] ...}
	//		[8:20 AM] Steve Ebersole: where `{[something]` triggers recognition of a literal
	//		[8:20 AM] Steve Ebersole: and `[something]` is a key to some registered resolver
	//		[8:21 AM] Steve Ebersole: e.g. for `{ts '2017-04-26 ...'}` we'd grab the timestamp literal handler
	//		[8:21 AM] Steve Ebersole: because of the `ts`
	//
	interface CustomExpressionTypeResolver {
		BasicValuedExpressableType resolveArithmeticType(
				BasicValuedExpressableType firstType,
				BasicValuedExpressableType secondType,
				boolean isDivision);

		BasicValuedExpressableType resolveSumFunctionType(BasicValuedExpressableType argumentType);

		BasicType resolveCastTargetType(String name);
	}
	//
	// A related discussion is recognition of a literal in the HQL, specifically for custom types.  From the same HipChat discussion:
	//		- allowing custom literal types becomes	easy(er) if we follow the escape-like syntax:
	//		- `{[something] ...}`
	//		- where `{` triggers recognition of a literal (by convention)
	//		- and `[something]` is a key to a registered (custom) resolver
	//
	interface HqlLiteralResolver {
		String getKey();

		<T> LiteralSqmExpression<T> resolveLiteral(String literal);
	}
	//
	//		I say related because both deal with custom user types as used in a SQM.

	public BasicValuedExpressableType resolveArithmeticType(
			BasicValuedExpressableType firstType,
			BasicValuedExpressableType secondType,
			org.hibernate.query.sqm.tree.expression.BinaryArithmeticSqmExpression.Operation operation) {
		return resolveArithmeticType( firstType, secondType, operation == BinaryArithmeticSqmExpression.Operation.DIVIDE );
	}

	/**
	 * Determine the result type of an arithmetic operation as defined by the
	 * rules in section 6.5.7.1.
	 * <p/>
	 *
	 *
	 * @return The operation result type
	 */
	public BasicValuedExpressableType resolveArithmeticType(
			BasicValuedExpressableType firstType,
			BasicValuedExpressableType secondType,
			boolean isDivision) {

		if ( isDivision ) {
			// covered under the note in 6.5.7.1 discussing the unportable
			// "semantics of the SQL division operation"..
			return getBasicTypeRegistry().getBasicType( Number.class );
		}


		// non-division

		if ( matchesJavaType( firstType, Double.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Double.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Float.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Float.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, BigDecimal.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, BigDecimal.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, BigInteger.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, BigInteger.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Long.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Long.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Integer.class ) ) {
			return firstType;
		}
		else if ( matchesJavaType( secondType, Integer.class ) ) {
			return secondType;
		}
		else if ( matchesJavaType( firstType, Short.class ) ) {
			return getBasicTypeRegistry().getBasicType( Integer.class );
		}
		else if ( matchesJavaType( secondType, Short.class ) ) {
			return getBasicTypeRegistry().getBasicType( Integer.class );
		}
		else {
			return getBasicTypeRegistry().getBasicType( Number.class );
		}
	}

	@SuppressWarnings("unchecked")
	private static boolean matchesJavaType(BasicValuedExpressableType type, Class javaType) {
		assert javaType != null;
		return type != null && javaType.isAssignableFrom( type.getJavaType() );
	}

	public BasicValuedExpressableType resolveSumFunctionType(BasicValuedExpressableType argumentType) {
			if ( matchesJavaType( argumentType, Double.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, Float.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, BigDecimal.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, BigInteger.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, Long.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, Integer.class ) ) {
				return argumentType;
			}
			else if ( matchesJavaType( argumentType, Short.class ) ) {
				return getBasicTypeRegistry().getBasicType( Integer.class );
			}
			else {
				return getBasicTypeRegistry().getBasicType( Number.class );
			}

	}

	public BasicType resolveCastTargetType(String name) {
		throw new NotYetImplementedException(  );
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

		// todo (6.0) : consider a proper contract implemented by both SessionFactory (or its metamodel) and boot's MetadataImplementor
		//		1) type-related info from MetadataBuildingOptions
		//		2) ServiceRegistry
		private transient MetadataBuildingContext metadataBuildingContext;
		private transient SessionFactoryImplementor sessionFactory;

		private String sessionFactoryName;
		private String sessionFactoryUuid;

		public MetadataBuildingContext getMetadataBuildingContext() {
			if ( metadataBuildingContext == null ) {
				throw new HibernateException( "TypeConfiguration is not currently scoped to MetadataBuildingContext" );
			}
			return metadataBuildingContext;
		}

		public void setMetadataBuildingContext(MetadataBuildingContext metadataBuildingContext) {
			this.metadataBuildingContext = metadataBuildingContext;
		}


		public SessionFactoryImplementor getSessionFactory() {
			if ( sessionFactory == null ) {
				if ( sessionFactoryName == null && sessionFactoryUuid == null ) {
					throw new HibernateException( "TypeConfiguration was not yet scoped to SessionFactory" );
				}
				sessionFactory = (SessionFactoryImplementor) SessionFactoryRegistry.INSTANCE.findSessionFactory(
						sessionFactoryUuid,
						sessionFactoryName
				);
				if ( sessionFactory == null ) {
					throw new HibernateException(
							"Could not find a SessionFactory [uuid=" + sessionFactoryUuid + ",name=" + sessionFactoryName + "]"
					);
				}

			}

			return sessionFactory;
		}

		/**
		 * Used by TypeFactory scoping.
		 *
		 * @param factory The SessionFactory that the TypeFactory is being bound to
		 */
		void setSessionFactory(SessionFactoryImplementor factory) {
			if ( this.sessionFactory != null ) {
				log.scopingTypesToSessionFactoryAfterAlreadyScoped( this.sessionFactory, factory );
			}
			else {
				this.metadataBuildingContext = null;

				this.sessionFactoryUuid = factory.getUuid();
				String sfName = factory.getSessionFactoryOptions().getSessionFactoryName();
				if ( sfName == null ) {
					final CfgXmlAccessService cfgXmlAccessService = factory.getServiceRegistry()
							.getService( CfgXmlAccessService.class );
					if ( cfgXmlAccessService.getAggregatedConfig() != null ) {
						sfName = cfgXmlAccessService.getAggregatedConfig().getSessionFactoryName();
					}
				}
				this.sessionFactoryName = sfName;
			}
			this.sessionFactory = factory;
		}

		public void unsetSessionFactory(SessionFactory factory) {
			log.debugf( "Un-scoping TypeConfiguration [%s] from SessionFactory [%s]", this, factory );
			this.sessionFactory = null;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SF-based initialization

	public void register(EntityDescriptor entityPersister) {
		entityPersisterMap.put( entityPersister.getEntityName(), entityPersister );
		entityHierarchies.add( entityPersister.getHierarchy() );

		if ( entityPersister.getConcreteProxyClass() != null
				&& entityPersister.getConcreteProxyClass().isInterface()
				&& !Map.class.isAssignableFrom( entityPersister.getConcreteProxyClass() )
				&& entityPersister.getMappedClass() != entityPersister.getConcreteProxyClass() ) {
			// IMPL NOTE : we exclude Map based proxy interfaces here because that should
			//		indicate MAP entity mode.0

			if ( entityPersister.getMappedClass().equals( entityPersister.getConcreteProxyClass() ) ) {
				// this part handles an odd case in the Hibernate test suite where we map an interface
				// as the class and the proxy.  I cannot think of a real life use case for that
				// specific test, but..
				log.debugf(
						"Entity [%s] mapped same interface [%s] as class and proxy",
						entityPersister.getEntityName(),
						entityPersister.getMappedClass()
				);
			}
			else {
				final JavaTypeDescriptor proxyInterfaceJavaDescriptor = getJavaTypeDescriptorRegistry().getDescriptor( entityPersister.getConcreteProxyClass() );
				final String old = entityProxyInterfaceMap.put( proxyInterfaceJavaDescriptor, entityPersister.getEntityName() );
				if ( old != null ) {
					throw new HibernateException(
							String.format(
									Locale.ENGLISH,
									"Multiple entities [%s, %s] named the same interface [%s] as their proxy which is not supported",
									old,
									entityPersister.getEntityName(),
									entityPersister.getConcreteProxyClass().getName()
							)
					);
				}
			}
		}

		registerEntityNameResolvers( entityPersister );
	}

	public void register(PersistentCollectionDescriptor collectionPersister) {
		collectionPersisterMap.put( collectionPersister.getNavigableRole().getFullPath(), collectionPersister );

		if ( collectionPersister.getIndexDescriptor() != null
				&& collectionPersister.getIndexDescriptor() instanceof EntityValuedExpressableType ) {
			final String entityName = ( (EntityValuedExpressableType) collectionPersister.getIndexDescriptor() ).getEntityName();
			final Set<String> roles = collectionRolesByEntityParticipant.computeIfAbsent(
					entityName,
					k -> new HashSet<>()
			);
			roles.add( collectionPersister.getNavigableRole().getFullPath() );
		}

		if ( collectionPersister.getElementDescriptor() instanceof EntityValuedExpressableType ) {
			final String entityName = ( (EntityValuedExpressableType) collectionPersister.getElementDescriptor() ).getEntityName();
			final Set<String> roles = collectionRolesByEntityParticipant.computeIfAbsent(
					entityName,
					k -> new HashSet<>()
			);
			roles.add( collectionPersister.getNavigableRole().getFullPath() );
		}
	}

	public void register(EmbeddedTypeDescriptor embeddablePersister) {
		embeddablePersisterMap.put( embeddablePersister.getRoleName(), embeddablePersister );

		final Set<String> roles = embeddedRolesByEmbeddableType.computeIfAbsent(
				embeddablePersister.getJavaTypeDescriptor(),
				k -> ConcurrentHashMap.newKeySet()
		);
		roles.add( embeddablePersister.getNavigableRole().getNavigableName() );
	}

	private void registerEntityNameResolvers(EntityDescriptor persister) {
		this.entityNameResolvers.addAll( persister.getEntityNameResolvers() );
	}

}