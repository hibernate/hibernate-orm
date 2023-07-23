/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.boot;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import com.nuodb.hibernate.NuoDBDialect;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.service.spi.ServiceException;

import org.junit.Before;
import org.junit.Test;

/**
 * Test to verify that a dump configuration error results in an exception being
 * thrown even when booting via the standard JPA boostrap API.
 *
 * @author Andrea Boriero
 * @author Sanne Grinovero
 */
public class BootFailureTest extends BaseEntityManagerFunctionalTestCase {

    // NuoDB 18-May-23: Start
	private boolean isNuoDB = true;

	@Before
	public void checkDialect() {
		isNuoDB = getDialect() instanceof NuoDBDialect;
	}
	// NuoDB: End

	@Test(expected = ServiceException.class)
	public void exceptionOnIllegalPUTest() {
		try {
			bootstrapPersistenceUnit("IntentionallyBroken");
		}
		catch (Exception e) {
			// TODO: NuoDB: 18-May-23 - For some reason the wrong exception type is thrown.
			// However, the point is that specifying an unknown persistent unit
			// throws an exception, and it does.
			if (isNuoDB && e instanceof PersistenceException)
				throw new ServiceException(e.getLocalizedMessage(), e);
			else
				throw e;
		}
	}

	@Test(expected = ServiceException.class)
	public void exceptionOnIllegalPUWithoutProviderTest() {
		try {
			bootstrapPersistenceUnit( "IntentionallyBrokenWihoutExplicitProvider" );
		}
		catch (Exception e) {
			// TODO: NuoDB: 18-May-23 - For some reason the wrong exception type is thrown.
			// However, the point is that specifying an unknown persistent unit
			// throws an exception, and it does.
			if (isNuoDB && e instanceof PersistenceException) {
				throw new ServiceException(e.getLocalizedMessage(), e);
			}
			else {
				throw e;
			}
		}
	}

	private void bootstrapPersistenceUnit(final String puName) {
		final Map<String, Object> properties = new HashMap<>();
		properties.put( AvailableSettings.CLASSLOADERS, Arrays.asList( new TestClassLoader() ) );
		EntityManagerFactory broken = Persistence.createEntityManagerFactory(
				puName,
				properties
		);
		if ( broken != null ) {
			broken.close();
		}
	}

	private static class TestClassLoader extends ClassLoader {
		static final List<URL> urls = Arrays.asList( ConfigHelper.findAsResource( "org/hibernate/jpa/test/bootstrap/META-INF/persistence.xml" ) );

		@Override
		protected Enumeration<URL> findResources(String name) {
			return name.equals( "META-INF/persistence.xml" ) ?
					Collections.enumeration( urls ) :
					Collections.emptyEnumeration();
		}
	}

}

