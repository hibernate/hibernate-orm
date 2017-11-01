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
import org.hibernate.NotYetImplementedFor6Exception;
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
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
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

import org.jboss.logging.Logger;

import static org.hibernate.internal.CoreLogging.logger;
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
	private static final Logger dev_log = logger( Scope.class );

	// todo : (
	private final Scope scope;
	private boolean initialized = false;

	// things available during both boot and runtime ("active") lifecycle phases
	private final JavaTypeDescriptorRegistry javaTypeDescriptorRegistry;
	private final SqlTypeDescriptorRegistry sqlTypeDescriptorRegistry;
	private final BasicTypeRegistry basicTypeRegistry;

	private final Set<EntityHierarchy> entityHierarchies = ConcurrentHashMap.newKeySet();
	private final Map<String,EntityDescriptor<?>> entityDescriptorMap = new ConcurrentHashMap<>();
	private final Map<String, MappedSuperclassDescriptor> mappedSuperclassDescriptorMap = new ConcurrentHashMap<>();
	private final Map<String,PersistentCollectionDescriptor<?,?,?>> collectionDescriptorMap = new ConcurrentHashMap<>();
	private final Map<String,EmbeddedTypeDescriptor<?>> embeddableDescriptorMap = new ConcurrentHashMap<>();

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
		StandardBasicTypes.prime( this );
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
		final EntityDescriptor<? extends T> entityDescriptor = findEntityDescriptor( entityClass );
		if ( entityDescriptor == null ) {
			throw new IllegalArgumentException( "Given class is not an entity : " + entityClass.getName() );
		}

		final List<EntityGraph<? super T>> results = new ArrayList<>();

		for ( EntityGraph entityGraph : entityGraphMap.values() ) {
			if ( !EntityGraphImplementor.class.isInstance( entityGraph ) ) {
				continue;
			}

			final EntityGraphImplementor egi = (EntityGraphImplementor) entityGraph;
			if ( egi.appliesTo( entityDescriptor ) ) {
				results.add( egi );
			}
		}

		return results;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityDescriptor access

	/**
	 * Retrieve all EntityDescriptors, keyed by entity-name
	 */
	public Map<String,EntityDescriptor<?>> getEntityDescriptorMap() {
		return Collections.unmodifiableMap( entityDescriptorMap );
	}

	/**
	 * Retrieve all EntityDescriptors
	 */
	public Collection<EntityDescriptor<?>> getEntityDescriptors() {
		return entityDescriptorMap.values();
	}

	/**
	 * Retrieve an EntityDescriptor by entity-name.  Returns {@code null} if not known.
	 */
	@SuppressWarnings("unchecked")
	public <T> EntityDescriptor<T> findEntityDescriptor(String entityName) {
		if ( importMap.containsKey( entityName ) ) {
			entityName = importMap.get( entityName );
		}
		return (EntityDescriptor<T>) entityDescriptorMap.get( entityName );
	}

	/**
	 * Retrieve an EntityDescriptor by entity-name.  Throws exception if not known.
	 */
	public <T> EntityDescriptor<T> resolveEntityDescriptor(String entityName) throws UnknownEntityTypeException {
		final EntityDescriptor<T> resolved = findEntityDescriptor( entityName );
		if ( resolved != null ) {
			return resolved;
		}

		throw new UnknownEntityTypeException( "Could not resolve EntityDescriptor by entity name [" + entityName + "]" );
	}

	public <T> EntityDescriptor<? extends T> findEntityDescriptor(Class<T> javaType) {
		EntityDescriptor<? extends T> entityDescriptor = findEntityDescriptor( javaType.getName() );
		if ( entityDescriptor == null ) {
			JavaTypeDescriptor javaTypeDescriptor = getJavaTypeDescriptorRegistry().getDescriptor( javaType );
			if ( javaTypeDescriptor != null && javaTypeDescriptor instanceof MappedSuperclassJavaDescriptor ) {
				String mappedEntityName = entityProxyInterfaceMap.get( javaTypeDescriptor );
				if ( mappedEntityName != null ) {
					entityDescriptor = findEntityDescriptor( mappedEntityName );
				}
			}
		}

		return entityDescriptor;
	}

	public <T> EntityDescriptor<? extends T> resolveEntityDescriptor(Class<T> javaType) {
		final EntityDescriptor<? extends T> entityDescriptor = findEntityDescriptor( javaType );
		if ( entityDescriptor == null ) {
			throw new UnknownEntityTypeException( "Could not resolve EntityDescriptor by Java type [" + javaType.getName() + "]" );
		}
		return entityDescriptor;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CollectionDesriptor access

	/**
	 * Retrieve all CollectionDesriptors, keyed by role (path)
	 */
	public Map<String,PersistentCollectionDescriptor<?,?,?>> getCollectionDescriptorMap() {
		return Collections.unmodifiableMap( collectionDescriptorMap );
	}

	/**
	 * Retrieve all CollectionDesriptors
	 */
	public Collection<PersistentCollectionDescriptor<?,?,?>> getCollectionDescriptors() {
		return collectionDescriptorMap.values();
	}

	/**
	 * Locate a CollectionDesriptor by role (path).  Returns {@code null} if not known
	 */
	@SuppressWarnings("unchecked")
	public <O,C,E> PersistentCollectionDescriptor<O,C,E> findCollectionDescriptor(String roleName) {
		return (PersistentCollectionDescriptor<O, C, E>) collectionDescriptorMap.get( roleName );
	}

	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return collectionRolesByEntityParticipant.get( entityName );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableDescriptor access

	public Collection<EmbeddedTypeDescriptor<?>> getEmbeddableDescriptors() {
		return embeddableDescriptorMap.values();
	}

	@SuppressWarnings("unchecked")
	public <T> EmbeddedTypeDescriptor<T> findEmbeddableDescriptor(String roleName) {
		return (EmbeddedTypeDescriptor<T>) embeddableDescriptorMap.get( roleName );
	}

	public <T> EmbeddedTypeDescriptor<T> findEmbeddableDescriptor(Class<T> javaType) {
		final JavaTypeDescriptor javaTypeDescriptor = getJavaTypeDescriptorRegistry().getDescriptor( javaType );
		if ( javaType == null || !EmbeddableJavaDescriptor.class.isInstance( javaTypeDescriptor ) ) {
			return null;
		}

		final Set<String> roles = embeddedRolesByEmbeddableType.get( javaTypeDescriptor );
		if ( roles == null || roles.isEmpty() || roles.size() > 1 ) {
			return null;
		}

		return findEmbeddableDescriptor( roles.iterator().next() );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQM Query handling
	//		- everything within this "block" of methods relates to SQM
	// 			interpretation of queries and implements its calls accordingly

	@SuppressWarnings("unchecked")
	public <T> EntityValuedExpressableType<T> resolveEntityReference(String name) {
		final String rename = importMap.get( name );
		if ( rename != null ) {
			name = rename;
		}

		{
			final EntityDescriptor descriptor = findEntityDescriptor( name );
			if ( descriptor != null ) {
				return descriptor;
			}
		}

		{
			final MappedSuperclassDescriptor descriptor = mappedSuperclassDescriptorMap.get( name );
			if ( descriptor != null ) {
				// todo (6.0) : a better option is to have MappedSuperclassDescriptor extend EntityValuedExpressableType
				//		but that currently causes some conflicts regarding `#getJavaTypeDescriptor`
				throw new NotYetImplementedFor6Exception();
			}
		}

		final Class requestedClass = resolveRequestedClass( name );
		if ( requestedClass != null ) {
			return resolveEntityReference( requestedClass );
		}

		throw new IllegalArgumentException( "Per JPA spec : no entity named " + name );
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
		// see if we know of this Class by name as an EntityDescriptor key
		if ( getEntityDescriptorMap().containsKey( javaType.getName() ) ) {
			// and if so, return that descriptor
			return (EntityValuedExpressableType<T>) getEntityDescriptorMap().get( javaType.getName() );
		}

		final JavaTypeDescriptor<T> jtd = getJavaTypeDescriptorRegistry().getDescriptor( javaType );
		if ( jtd == null ) {
			throw new HibernateException( "Could not locate JavaTypeDescriptor : " + javaType.getName() );
		}

		// next check entityProxyInterfaceMap
		final String proxyEntityName = entityProxyInterfaceMap.get( jtd );
		if ( proxyEntityName != null ) {
			return (EntityValuedExpressableType<T>) getEntityDescriptorMap().get( proxyEntityName );
		}

		// otherwise, trye to handle it as a polymorphic reference
		if ( polymorphicEntityReferenceMap.containsKey( jtd ) ) {
			return (EntityValuedExpressableType<T>) polymorphicEntityReferenceMap.get( jtd );
		}

		final Set<EntityDescriptor<?>> implementors = getImplementors( javaType );
		if ( !implementors.isEmpty() ) {
			final PolymorphicEntityValuedExpressableTypeImpl entityReference = new PolymorphicEntityValuedExpressableTypeImpl(
					jtd,
					implementors
			);
			polymorphicEntityReferenceMap.put( jtd, entityReference );
			return entityReference;
		}

		throw new IllegalArgumentException( "Could not resolve entity reference : " + javaType.getName() );
	}

	@SuppressWarnings("unchecked")
	public Set<EntityDescriptor<?>> getImplementors(Class javaType) {
		// if the javaType refers directly to an EntityDescriptor by Class name, return just it.
		final EntityDescriptor<?> exactMatch = getEntityDescriptorMap().get( javaType.getName() );
		if ( exactMatch != null ) {
			return Collections.singleton( exactMatch );
		}

		final HashSet<EntityDescriptor<?>> matchingDescriptors = new HashSet<>();

		for ( EntityDescriptor entityDescriptor : getEntityDescriptorMap().values() ) {
			if ( entityDescriptor.getJavaType() == null ) {
				continue;
			}

			// todo : explicit/implicit polymorphism...
			// todo : handle "duplicates" within a hierarchy
			// todo : in fact we may want to cycle through descriptors via entityHierarchies and walking the subclass graph rather than walking each descriptor linearly (in random order)

			if ( javaType.isAssignableFrom( entityDescriptor.getJavaType() ) ) {
				matchingDescriptors.add( entityDescriptor );
			}
		}

		return matchingDescriptors;
	}



















	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Scoping


	/**
	 * Obtain the MetadataBuildingContext currently scoping the
	 * TypeConfiguration.
	 *
	 * @apiNote This will throw an exception if the SessionFactory is not yet
	 * bound here.  See {@link Scope} for more details regarding the stages
	 * a TypeConfiguration goes through
	 *
	 * @return
	 */
	public MetadataBuildingContext getMetadataBuildingContext() {
		return scope.getMetadataBuildingContext();
	}

	public void scope(MetadataBuildingContext metadataBuildingContext) {
		log.debugf( "Scoping TypeConfiguration [%s] to MetadataBuildingContext [%s]", this, metadataBuildingContext );
		scope.setMetadataBuildingContext( metadataBuildingContext );
	}

	/**
	 * Obtain the SessionFactory currently scoping the TypeConfiguration.
	 *
	 * @apiNote This will throw an exception if the SessionFactory is not yet
	 * bound here.  See {@link Scope} for more details regarding the stages
	 * a TypeConfiguration goes through (this is "runtime stage")
	 *
	 * @return The SessionFactory
	 *
	 * @throws IllegalStateException if the TypeConfiguration is currently not
	 * associated with a SessionFactory (in "runtime stage").
	 */
	public SessionFactoryImplementor getSessionFactory() {
		return scope.getSessionFactory();
	}

	public void scope(SessionFactoryImplementor factory, BootstrapContext bootstrapContext) {
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactory [%s]", this, factory );

		for ( Map.Entry<String, String> importEntry : scope.metadataBuildingContext.getMetadataCollector().getImports().entrySet() ) {
			if ( importMap.containsKey( importEntry.getKey() ) ) {
				continue;
			}

			importMap.put( importEntry.getKey(), importEntry.getValue() );
		}

		scope.setSessionFactory( factory );
		factory.addObserver( this );

		new RuntimeModelCreationProcess( factory, bootstrapContext, getMetadataBuildingContext() ).execute();
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		// Instead of allowing scope#setSessionFactory to influence this, we use the SessionFactoryObserver callback
		// to handle this, allowing any SessionFactory constructor code to be able to continue to have access to the
		// MetadataBuildingContext through TypeConfiguration until this callback is fired.
		log.tracef( "Handling #sessionFactoryCreated from [%s] for TypeConfiguration", factory );
		scope.setMetadataBuildingContext( null );
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		log.tracef( "Handling #sessionFactoryClosed from [%s] for TypeConfiguration", factory );
		scope.unsetSessionFactory( factory );

		// todo (6.0) : finish this
		//		release Database, descriptor Maps, etc... things that are only
		// 		valid while the TypeConfiguration is scoped to SessionFactory
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

		<T> SqmLiteral<T> resolveLiteral(String literal);
	}
	//
	//		I say related because both deal with custom user types as used in a SQM.

	public BasicValuedExpressableType resolveArithmeticType(
			BasicValuedExpressableType firstType,
			BasicValuedExpressableType secondType,
			SqmBinaryArithmetic.Operation operation) {
		return resolveArithmeticType( firstType, secondType, operation == SqmBinaryArithmetic.Operation.DIVIDE );
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
	 * Encapsulation of lifecycle concerns for a TypeConfiguration, mainly in
	 * regards to eventually being associated with a SessionFactory.  Goes
	 * 3 "lifecycle" stages, pertaining to {@link #getMetadataBuildingContext()}
	 * and {@link #getSessionFactory()}:
	 *
	 * 		* "Initialization" is where the {@link TypeConfiguration} is first
	 * 			built as the "boot model" ({@link org.hibernate.boot.model}) of
	 * 			the user's domain model is converted into the "runtime model"
	 * 			({@link org.hibernate.metamodel.model}).  During this phase,
	 * 			{@link #getMetadataBuildingContext()} will be accessible but
	 * 			{@link #getSessionFactory} will throw an exception.
	 * 		* "Runtime" is where the "runtime model" is accessible while the
	 * 			SessionFactory is still unclosed.  During this phase
	 * 			{@link #getSessionFactory()} is accessible while
	 * 			{@link #getMetadataBuildingContext()} will now throw an
	 * 			exception
	 * 		* "Sunset" is after the SessionFactory has been closed.  During this
	 * 			phase both {@link #getSessionFactory()} and
	 * 			{@link #getMetadataBuildingContext()} will now throw an exception
	 *
	 * Each stage or phase is consider a "scope" for the TypeConfiguration.
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

	public void register(EntityHierarchy hierarchy) {
		entityHierarchies.add( hierarchy );
	}

	public void register(EntityDescriptor entityDescriptor) {
		entityDescriptorMap.put( entityDescriptor.getEntityName(), entityDescriptor );

		if ( entityDescriptor.getConcreteProxyClass() != null
				&& entityDescriptor.getConcreteProxyClass().isInterface()
				&& !Map.class.isAssignableFrom( entityDescriptor.getConcreteProxyClass() )
				&& entityDescriptor.getMappedClass() != entityDescriptor.getConcreteProxyClass() ) {
			// IMPL NOTE : we exclude Map based proxy interfaces here because that should
			//		indicate MAP entity mode.0

			if ( entityDescriptor.getMappedClass().equals( entityDescriptor.getConcreteProxyClass() ) ) {
				// this part handles an odd case in the Hibernate test suite where we map an interface
				// as the class and the proxy.  I cannot think of a real life use case for that
				// specific test, but..
				log.debugf(
						"Entity [%s] mapped same interface [%s] as class and proxy",
						entityDescriptor.getEntityName(),
						entityDescriptor.getMappedClass()
				);
			}
			else {
				final JavaTypeDescriptor proxyInterfaceJavaDescriptor = getJavaTypeDescriptorRegistry().getDescriptor( entityDescriptor.getConcreteProxyClass() );
				final String old = entityProxyInterfaceMap.put( proxyInterfaceJavaDescriptor, entityDescriptor.getEntityName() );
				if ( old != null ) {
					throw new HibernateException(
							String.format(
									Locale.ENGLISH,
									"Multiple entities [%s, %s] named the same interface [%s] as their proxy which is not supported",
									old,
									entityDescriptor.getEntityName(),
									entityDescriptor.getConcreteProxyClass().getName()
							)
					);
				}
			}
		}

		registerEntityNameResolvers( entityDescriptor );
	}

	public void register(MappedSuperclassDescriptor runtimeType) {
		mappedSuperclassDescriptorMap.put(
				runtimeType.getJavaTypeDescriptor().getTypeName(),
				runtimeType
		);
	}

	public void register(PersistentCollectionDescriptor collectionDescriptor) {
		collectionDescriptorMap.put( collectionDescriptor.getNavigableRole().getFullPath(), collectionDescriptor );

		if ( collectionDescriptor.getIndexDescriptor() != null
				&& collectionDescriptor.getIndexDescriptor() instanceof EntityValuedExpressableType ) {
			final String entityName = ( (EntityValuedExpressableType) collectionDescriptor.getIndexDescriptor() ).getEntityName();
			final Set<String> roles = collectionRolesByEntityParticipant.computeIfAbsent(
					entityName,
					k -> new HashSet<>()
			);
			roles.add( collectionDescriptor.getNavigableRole().getFullPath() );
		}

		if ( collectionDescriptor.getElementDescriptor() instanceof EntityValuedExpressableType ) {
			final String entityName = ( (EntityValuedExpressableType) collectionDescriptor.getElementDescriptor() ).getEntityName();
			final Set<String> roles = collectionRolesByEntityParticipant.computeIfAbsent(
					entityName,
					k -> new HashSet<>()
			);
			roles.add( collectionDescriptor.getNavigableRole().getFullPath() );
		}
	}

	public void register(EmbeddedTypeDescriptor embeddedTypeDescriptor) {
		embeddableDescriptorMap.put( embeddedTypeDescriptor.getRoleName(), embeddedTypeDescriptor );

		final Set<String> roles = embeddedRolesByEmbeddableType.computeIfAbsent(
				embeddedTypeDescriptor.getJavaTypeDescriptor(),
				k -> ConcurrentHashMap.newKeySet()
		);
		roles.add( embeddedTypeDescriptor.getNavigableRole().getNavigableName() );
	}

	private void registerEntityNameResolvers(EntityDescriptor descriptor) {
		@SuppressWarnings("unchecked")
		final List<EntityNameResolver> resolvers = descriptor.getEntityNameResolvers();
		if ( resolvers != null ) {
			this.entityNameResolvers.addAll( resolvers );
		}
	}

	public Set<EntityHierarchy> getEntityHierarchies() {
		return entityHierarchies;
	}
}