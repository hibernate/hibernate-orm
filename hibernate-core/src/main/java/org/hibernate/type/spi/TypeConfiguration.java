/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.sqm.domain.BasicType;
import org.hibernate.type.spi.basic.BasicTypeRegistry;
import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptorRegistry;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptorRegistry;

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

	public TypeDescriptorRegistryAccess getTypeDescriptorRegistryAccess() {
		return this;
	}

	public BasicTypeRegistry getBasicTypeRegistry() {
		return basicTypeRegistry;
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


	/**
	 * Encapsulation of lifecycle concerns for a TypeConfiguration, mainly in regards to
	 * eventually being associated with a SessionFactory.
	 */
	private static class Scope {
		private transient Mapping mapping;

		private String sessionFactoryName;
		private String sessionFactoryUuid;

		Scope(Mapping mapping) {
			this.mapping = mapping;
		}

		public Mapping getMapping() {
			return mapping;
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
}
