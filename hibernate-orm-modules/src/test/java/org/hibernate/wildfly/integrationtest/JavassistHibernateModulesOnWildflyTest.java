/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.wildfly.integrationtest;

import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.hibernate.bytecode.internal.javassist.BytecodeProviderImpl;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.wildfly.model.Kryptonite;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceUnitTransactionType;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * The purpose of this test is to check that it's still possible to use Javassist as byte code provider with WildFly.
 */
@RunWith(Arquillian.class)
public class JavassistHibernateModulesOnWildflyTest {

	private static final String ORM_VERSION = Session.class.getPackage().getImplementationVersion();
	private static final String ORM_MINOR_VERSION = ORM_VERSION.substring( 0, ORM_VERSION.indexOf( ".", ORM_VERSION.indexOf( "." ) + 1) );

	@Deployment
	public static WebArchive createDeployment() {
		return ShrinkWrap.create( WebArchive.class )
				.addClass( Kryptonite.class )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" );
	}

	private static Asset persistenceXml() {
		PersistenceDescriptor persistenceXml = Descriptors.create( PersistenceDescriptor.class )
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
		return new StringAsset( persistenceXml.exportAsString() );
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

	@Test
	public void shouldBeAbleToCreateProxyWithJavassist() {
		Properties properties = new Properties();
		properties.setProperty( AvailableSettings.BYTECODE_PROVIDER, Environment.BYTECODE_PROVIDER_NAME_JAVASSIST );

		// hibernate.bytecode.provider is a system property. I don't want to apply it
		// to the arquillian.xml because it will change the other tests as well.
		// I guess this is a more explicit way anyway to test that Javassist is available.
		BytecodeProvider provider = Environment.buildBytecodeProvider( properties );
		assertThat( provider.getClass(), equalTo( BytecodeProviderImpl.class ) );

		ProxyFactoryFactory factory = provider.getProxyFactoryFactory();
		BasicProxyFactory basicProxyFactory = factory.buildBasicProxyFactory( Kryptonite.class, null );
		Object proxy = basicProxyFactory.getProxy();
		assertThat( proxy, notNullValue() );
	}
}
