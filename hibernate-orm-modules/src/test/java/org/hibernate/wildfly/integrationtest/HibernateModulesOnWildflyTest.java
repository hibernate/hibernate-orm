/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.wildfly.integrationtest;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceUnitTransactionType;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration test for using the current Hibernate ORM version on WildFly.
 * <p>
 * Gradle will unzip the targeted WildFly version and unpack the module ZIP created by this build into the server's
 * module directory. Arquillian is used to start this WildFly instance, run this test on the server and stop the server
 * again.
 *
 * @author Gunnar Morling
 */
@RunWith(Arquillian.class)
public class HibernateModulesOnWildflyTest {

	private static final String ORM_VERSION = Session.class.getPackage().getImplementationVersion();
	private static final String ORM_MINOR_VERSION = ORM_VERSION.substring( 0, ORM_VERSION.indexOf( ".", ORM_VERSION.indexOf( "." ) + 1) );

	@Deployment
	public static WebArchive createDeployment() {
		return ShrinkWrap.create( WebArchive.class )
				.addClass( Kryptonite.class )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addAsWebInfResource( "jboss-deployment-structure.xml" )
				.addAsResource( new StringAsset( persistenceXml().exportAsString() ), "META-INF/persistence.xml" );
	}

	private static PersistenceDescriptor persistenceXml() {
		return Descriptors.create( PersistenceDescriptor.class )
					.version( "2.1" )
					.createPersistenceUnit()
						.name( "primary" )
						.transactionType( PersistenceUnitTransactionType._JTA )
						.jtaDataSource( "java:jboss/datasources/ExampleDS" )
						.getOrCreateProperties()
							// We want to use the ORM from this build instead of the one coming with WildFly
							.createProperty().name( "jboss.as.jpa.providerModule" ).value( "org.hibernate:" + ORM_MINOR_VERSION ).up()
							.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
							.createProperty().name( "hibernate.allow_update_outside_transaction" ).value( "true" ).up()
					.up().up();
	}

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	public void shouldUseHibernateOrm52() {
		Session session = entityManager.unwrap( Session.class );

		Kryptonite kryptonite1 = new Kryptonite();
		kryptonite1.id = 1L;
		kryptonite1.description = "Some Kryptonite";
		session.persist( kryptonite1 );

		// EntityManager methods exposed through Session only as of 5.2
		Kryptonite loaded = session.find( Kryptonite.class, 1L );

		assertThat( loaded.description, equalTo( "Some Kryptonite" ) );
	}
}
