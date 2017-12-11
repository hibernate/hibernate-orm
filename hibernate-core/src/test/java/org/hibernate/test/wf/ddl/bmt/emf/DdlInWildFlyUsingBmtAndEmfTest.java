/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.wf.ddl.bmt.emf;

import java.net.URL;
import java.util.Collections;
import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.Bootstrap;

import org.hibernate.test.wf.ddl.WildFlyDdlEntity;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceUnitTransactionType;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
@RunWith( Arquillian.class )
@Ignore( "WildFly has not released a version supporting JPA 2.2 and CDI 2.0" )
public class DdlInWildFlyUsingBmtAndEmfTest {

	public static final String PERSISTENCE_XML_RESOURCE_NAME = "pu-wf-ddl/persistence.xml";
	public static final String PERSISTENCE_UNIT_NAME = "pu-wf-ddl";

	@Deployment
	public static WebArchive buildDeployment() {
		WebArchive war = ShrinkWrap.create( WebArchive.class )
				.setManifest( "org/hibernate/test/wf/ddl/manifest.mf" )
				.addClass( WildFlyDdlEntity.class )
//				.addAsManifestResource( EmptyAsset.INSTANCE, "beans.xml")
				.addAsResource( new StringAsset( persistenceXml().exportAsString() ), PERSISTENCE_XML_RESOURCE_NAME )
				.addAsResource( "org/hibernate/test/wf/ddl/log4j.properties", "log4j.properties" );
		System.out.println( war.toString(true) );
		return war;
	}

	private static PersistenceDescriptor persistenceXml() {
		final PersistenceDescriptor pd = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.1" )
				.createPersistenceUnit().name( PERSISTENCE_UNIT_NAME )
				.transactionType( PersistenceUnitTransactionType._JTA )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				.clazz( WildFlyDdlEntity.class.getName() )
				.excludeUnlistedClasses( true )
				.getOrCreateProperties().createProperty().name( "jboss.as.jpa.providerModule" ).value( "org.hibernate:5.3" ).up().up()
				.getOrCreateProperties().createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up().up()
				// this should not be needed, but...
				.getOrCreateProperties().createProperty().name( AvailableSettings.JTA_PLATFORM ).value( JBossAppServerJtaPlatform.class.getName() ).up().up()
				.up();


		System.out.println( "persistence.xml: " );
		pd.exportTo( System.out );

		return pd;
	}

	@ArquillianResource
	private InitialContext initialContext;

	@Test
	public void testCreateThenDrop() throws Exception {
		URL persistenceXmlUrl = Thread.currentThread().getContextClassLoader().getResource( PERSISTENCE_XML_RESOURCE_NAME );
		if ( persistenceXmlUrl == null ) {
			persistenceXmlUrl = Thread.currentThread().getContextClassLoader().getResource( '/' + PERSISTENCE_XML_RESOURCE_NAME );
		}

		assertNotNull( persistenceXmlUrl );

		ParsedPersistenceXmlDescriptor persistenceUnit = PersistenceXmlParser.locateIndividualPersistenceUnit( persistenceXmlUrl );
		// creating the EMF causes SchemaCreator to be run...
		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( persistenceUnit, Collections.emptyMap() ).build();

		// closing the EMF causes the delayed SchemaDropper to be run...
		//		wrap in a transaction just to see if we can get this to fail in the way the WF report says;
		//		in my experience however this succeeds with or without the transaction
		final TransactionManager tm = emf.unwrap( SessionFactoryImplementor.class ).getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager();

		tm.begin();
		Transaction txn = tm.getTransaction();
		emf.close();
		txn.commit();
	}
}
