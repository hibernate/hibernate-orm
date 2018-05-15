/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.io.InvalidObjectException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.uuid.LocalObjectUuidHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationProcess;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry;
import org.hibernate.type.internal.TypeConfigurationRegistry;

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
 * @since 5.3
 */
@Incubating
public class TypeConfiguration implements SessionFactoryObserver, Serializable {
	private static final CoreMessageLogger log = messageLogger( Scope.class );

	private final String uuid = LocalObjectUuidHelper.generateLocalObjectUuid();

	private final Scope scope;
	private boolean initialized;

	// things available during both boot and runtime ("active") lifecycle phases
	private final transient JavaTypeDescriptorRegistry javaTypeDescriptorRegistry;
	private final transient SqlTypeDescriptorRegistry sqlTypeDescriptorRegistry;
	private final transient BasicTypeRegistry basicTypeRegistry;

	private final transient Map<SqlTypeDescriptor,Map<BasicJavaDescriptor, SqlExpressableType>> jdbcValueMapperCache = new ConcurrentHashMap<>();

	public TypeConfiguration() {
		this.scope = new Scope();

		this.javaTypeDescriptorRegistry = new JavaTypeDescriptorRegistry( this );
		this.sqlTypeDescriptorRegistry = new SqlTypeDescriptorRegistry( this );

		this.basicTypeRegistry = new BasicTypeRegistry( this );
		StandardBasicTypes.prime( this );

		this.initialized = true;

		TypeConfigurationRegistry.INSTANCE.registerTypeConfiguration( this );
	}

	public String getUuid() {
		return uuid;
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

	public BasicTypeRegistry getBasicTypeRegistry() {
		return basicTypeRegistry;
	}

	public SqlExpressableType resolveJdbcValueMapper(
			SqlTypeDescriptor sqlTypeDescriptor,
			BasicJavaDescriptor javaDescriptor,
			java.util.function.Function<BasicJavaDescriptor, SqlExpressableType> creator) {
		final Map<BasicJavaDescriptor, SqlExpressableType> cacheForSqlType = jdbcValueMapperCache.computeIfAbsent(
				sqlTypeDescriptor,
				x -> new ConcurrentHashMap<>()
		);

		return cacheForSqlType.computeIfAbsent( javaDescriptor, x -> creator.apply( javaDescriptor ) );
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
	 */
	public MetadataBuildingContext getMetadataBuildingContext() {
		return scope.getMetadataBuildingContext();
	}

	public void scope(MetadataBuildingContext metadataBuildingContext) {
		log.debugf( "Scoping TypeConfiguration [%s] to MetadataBuildingContext [%s]", this, metadataBuildingContext );
		scope.setMetadataBuildingContext( metadataBuildingContext );
	}

	public MetamodelImplementor scope(SessionFactoryImplementor sessionFactory, BootstrapContext bootstrapContext) {
		assert scope.metadataBuildingContext != null;
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactoryImpl [%s]", this, sessionFactory );
		scope.setSessionFactory( sessionFactory );
		sessionFactory.addObserver( this );
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactory [%s]", this, sessionFactory );

		return RuntimeModelCreationProcess.execute(
				sessionFactory,
				bootstrapContext,
				scope.getMetadataBuildingContext()
		);
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

	/**
	 * Obtain the ServiceRegistry scoped to the TypeConfiguration.
	 *
	 * @apiNote Depending on what the {@link Scope} is currently scoped to will determine where the
	 * {@link ServiceRegistry} is obtained from.
	 *
	 * @return The ServiceRegistry
	 */
	public ServiceRegistry getServiceRegistry() {
		return scope.getServiceRegistry();
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

		TypeConfigurationRegistry.INSTANCE.deregisterTypeConfiguration( this );

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
	private static class Scope implements Serializable {
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

		public ServiceRegistry getServiceRegistry() {
			if ( metadataBuildingContext != null ) {
				return metadataBuildingContext.getBootstrapContext().getServiceRegistry();
			}
			else if ( sessionFactory != null ) {
				return sessionFactory.getServiceRegistry();
			}
			return null;
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

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Custom serialization hook

		private Object readResolve() throws InvalidObjectException {
			if ( sessionFactory == null ) {
				if ( sessionFactoryName != null || sessionFactoryUuid != null ) {
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
			}

			return this;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Custom serialization hook

	private Object readResolve() throws InvalidObjectException {
		log.trace( "Resolving serialized TypeConfiguration - readResolve" );
		return TypeConfigurationRegistry.INSTANCE.findTypeConfiguration( getUuid() );
	}
}
