/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Encapsulation of lifecycle concerns for a TypeConfiguration, mainly in
 * regards to eventually being associated with a SessionFactory.  Goes
 * 3 "lifecycle" stages, pertaining to {@link TypeConfiguration#getMetadataBuildingContext()}
 * and {@link TypeConfiguration#getSessionFactory()}:
 *
 * 		* "Initialization" is where the {@link TypeConfiguration} is first
 * 			built as the "boot model" ({@link org.hibernate.boot.model}) of
 * 			the user's domain model is converted into the "runtime model"
 * 			({@link org.hibernate.metamodel.model}).  During this phase,
 * 			{@link TypeConfiguration#getMetadataBuildingContext()} will be accessible but
 * 			{@link TypeConfiguration#getSessionFactory} will throw an exception.
 * 		* "Runtime" is where the "runtime model" is accessible while the
 * 			SessionFactory is still unclosed.  During this phase
 * 			{@link TypeConfiguration#getSessionFactory()} is accessible while
 * 			{@link TypeConfiguration#getMetadataBuildingContext()} will now throw an
 * 			exception
 * 		* "Sunset" is after the SessionFactory has been closed.  During this
 * 			phase both {@link TypeConfiguration#getSessionFactory()} and
 * 			{@link TypeConfiguration#getMetadataBuildingContext()} will now throw an exception
 *
 * Each stage or phase is consider a "scope" for the {@link TypeConfiguration}.
 *
 *  @author Steve Ebersole
 *
 *  @since 5.3
 */
@Incubating
class Scope implements Serializable {// todo (6.0) : consider a proper contract implemented by both SessionFactory (or its metamodel) and boot's MetadataImplementor
	private static final CoreMessageLogger log = messageLogger( Scope.class );

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
