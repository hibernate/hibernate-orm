/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.fetch.depth;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.classloader.ShrinkWrapClassLoader;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import static jakarta.persistence.Persistence.createEntityManagerFactory;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;

/**
 * @author Steve Ebersole
 */
@JiraKey( "HHH-14993" )
public class NoDepthTests {

	@Test
	public void testWithMax() {
		testIt( true );
	}

	@Test
	public void testNoMax() {
		testIt( false );
	}

	private void testIt(boolean configureMaxDepth) {
		try ( final SessionFactoryImplementor sf = buildSessionFactory( configureMaxDepth ) ) {
			inTransaction( sf, (session) -> {
				session.createSelectionQuery( "from SysModule" ).getResultList();
				session.createSelectionQuery( "from SysModule2" ).getResultList();
			} );
		}
	}

	private static SessionFactoryImplementor buildSessionFactory(boolean configureMax) {
		final StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.applySetting( AvailableSettings.URL, "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;" );
		registryBuilder.applySetting( AvailableSettings.USER, "sa" );
		registryBuilder.applySetting( AvailableSettings.POOL_SIZE, "5" );
		registryBuilder.applySetting( AvailableSettings.FORMAT_SQL, "true" );
		registryBuilder.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		if ( configureMax ) {
			registryBuilder.applySetting( AvailableSettings.MAX_FETCH_DEPTH, "10" );
		}
		else {
			registryBuilder.applySetting( AvailableSettings.MAX_FETCH_DEPTH, "" );
		}

		return new MetadataSources( registryBuilder.build() )
				.addAnnotatedClasses( SysModule.class, SysModule2.class )
				.buildMetadata()
				.buildSessionFactory()
				.unwrap( SessionFactoryImplementor.class );
	}

	@Test
	public void testWithMaxJpa() {
		testItJpa( "with-max" );
	}

	@Test
	public void testNoMaxJpa() {
		testItJpa( "no-max" );
	}

	private void testItJpa(String unitName) {
		final JavaArchive par = ShrinkWrap.create( JavaArchive.class, unitName + ".par" );
		par.addClasses( SysModule.class );
		par.addAsResource( "units/many2many/fetch-depth.xml", "META-INF/persistence.xml" );

		try ( final ShrinkWrapClassLoader classLoader = new ShrinkWrapClassLoader( par ) ) {
			final Map<String, ?> settings = CollectionHelper.toMap(
					AvailableSettings.CLASSLOADERS,
					Arrays.asList( classLoader, getClass().getClassLoader() )
			);

			final EntityManagerFactory emf = createEntityManagerFactory( unitName, settings );
			try ( final SessionFactoryImplementor sf = emf.unwrap( SessionFactoryImplementor.class ) ) {
				// play around with the SF and make sure it is operable
				inTransaction( sf, (s) -> {
					s.createSelectionQuery( "from SysModule" ).list();
					s.createSelectionQuery( "from SysModule2" ).list();
				});
			}
		}
		catch (IOException e) {
			throw new RuntimeException( "re-throw", e );
		}
	}

}
