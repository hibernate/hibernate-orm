/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.wildfly.integrationtest;

import java.util.Arrays;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.wildfly.model.AuditedEntity;

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

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@RunWith(Arquillian.class)
public class HibernateEnversOnWildflyTest {

	private static final String ORM_VERSION = Session.class.getPackage().getImplementationVersion();
	private static final String ORM_MINOR_VERSION = ORM_VERSION.substring( 0, ORM_VERSION.indexOf( ".", ORM_VERSION.indexOf( "." ) + 1 ) );

	@Deployment
	public static WebArchive createDeployment() {
		return ShrinkWrap.create( WebArchive.class )
				.addClass( AuditedEntity.class )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
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

	@Inject
	private UserTransaction userTransaction;

	@Test
	public void testEnversCompatibility() throws Exception {
		// revision 1
		userTransaction.begin();
		entityManager.joinTransaction();
		AuditedEntity entity = new AuditedEntity( 1, "Marco Polo" );
		entityManager.persist( entity );
		userTransaction.commit();

		// revision 2
		userTransaction.begin();
		entityManager.joinTransaction();
		entity.setName( "George Washington" );
		entityManager.merge( entity );
		userTransaction.commit();

		entityManager.clear();

		// verify audit history revision counts
		userTransaction.begin();
		final AuditReader auditReader = AuditReaderFactory.get( entityManager );
		assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( AuditedEntity.class, 1 ) );
		userTransaction.commit();
	}
}
