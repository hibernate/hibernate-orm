/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.wildfly.integrationtest;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.wildfly.model.Kryptonite;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceUnitTransactionType;

import static org.junit.Assert.assertFalse;

/**
 * @author Andrea Boriero
 */
@RunWith(Arquillian.class)
public class TransactionRollbackTest {

	private static final String ORM_VERSION = Session.class.getPackage().getImplementationVersion();
	private static final String ORM_MINOR_VERSION = ORM_VERSION.substring( 0,
																		   ORM_VERSION.indexOf(
																				   ".",
																				   ORM_VERSION.indexOf( "." ) + 1
																		   )
	);

	@Deployment
	public static WebArchive createDeployment() {
		return ShrinkWrap.create( WebArchive.class )
				.addClass( Kryptonite.class )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addAsResource( new StringAsset( persistenceXml().exportAsString() ), "META-INF/persistence.xml" );
	}

	private static PersistenceDescriptor persistenceXml() {
		return Descriptors.create( PersistenceDescriptor.class )
				.version( "2.1" )
				.createPersistenceUnit()
				.name( "primary" )
				.transactionType( PersistenceUnitTransactionType._RESOURCE_LOCAL )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				.getOrCreateProperties()
				// We want to use the ORM from this build instead of the one coming with WildFly
				.createProperty()
				.name( "jboss.as.jpa.providerModule" )
				.value( "org.hibernate:" + ORM_MINOR_VERSION )
				.up()
				.createProperty()
				.name( "hibernate.hbm2ddl.auto" )
				.value( "create-drop" )
				.up()
				.createProperty()
				.name( "hibernate.allow_update_outside_transaction" )
				.value( "true" )
				.up()
				.up()
				.up();
	}

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	public void testMarkRollbackOnlyAnUnactiveTransaction() {
		EntityTransaction transaction = entityManager.getTransaction();
		final TransactionImplementor hibernateTransaction = (TransactionImplementor) transaction;
		hibernateTransaction.markRollbackOnly();
		transaction.rollback();
		assertFalse( transaction.isActive() );
	}

	@Test
	public void testMarkRollbackOnlyAnActiveTransaction() {
		EntityTransaction transaction = entityManager.getTransaction();
		final TransactionImplementor hibernateTransaction = (TransactionImplementor) transaction;
		transaction.begin();
		hibernateTransaction.markRollbackOnly();
		transaction.rollback();
		assertFalse( transaction.isActive() );
	}
}
