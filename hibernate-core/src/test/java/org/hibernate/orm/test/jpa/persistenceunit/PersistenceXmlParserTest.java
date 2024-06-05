/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.persistenceunit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.internal.util.ConfigHelper.findAsResource;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;

import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import jakarta.persistence.spi.PersistenceUnitTransactionType;

@SuppressWarnings("removal")
@JiraKey("HHH-18231")
public class PersistenceXmlParserTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, PersistenceXmlParser.class.getName() )
	);

	@Test
	public void create_classLoaders() {
		var parser = PersistenceXmlParser.create(
				Map.of( AvailableSettings.CLASSLOADERS, Arrays.asList( new TestClassLoader( "pu1" ) ) ),
				new TestClassLoader( "pu2" ),
				null
		);
		assertThat( parser.getClassLoaderService() ).isNotNull();
		assertThat( parser.locatePersistenceUnits() )
				.extracting( PersistenceUnitDescriptor::getName )
				.containsExactlyInAnyOrder( "pu1", "pu2" );
	}

	@Test
	public void create_classLoaderService() {
		var myClassLoaderService = new ClassLoaderServiceImpl( new TestClassLoader( "pu3" ) );
		var parser = PersistenceXmlParser.create(
				// Should be ignored
				Map.of( AvailableSettings.CLASSLOADERS, Arrays.asList( new TestClassLoader( "pu1" ) ) ),
				// Should be ignored
				new TestClassLoader( "pu2" ),
				myClassLoaderService
		);
		assertThat( parser.getClassLoaderService() ).isSameAs( myClassLoaderService );
		assertThat( parser.locatePersistenceUnits() )
				.extracting( PersistenceUnitDescriptor::getName )
				.containsExactlyInAnyOrder( "pu3" );
	}

	@Test
	public void locatePersistenceUnits() {
		var parser = PersistenceXmlParser.create(
				Map.of(),
				new TestClassLoader( "pu1" ),
				null
		);
		assertThat( parser.locatePersistenceUnits() )
				.singleElement()
				.returns( "pu1", PersistenceUnitDescriptor::getName );
	}

	@Test
	public void locatePersistenceUnits_empty() {
		var noFileLog = logInspection.watchForLogMessages( "HHH000318" );
		var parser = PersistenceXmlParser.create();
		assertThat( parser.locatePersistenceUnits() ).isEmpty();
		assertThat( noFileLog.wasTriggered() ).isTrue();
	}

	@Test
	public void parse() {
		var parser = PersistenceXmlParser.create();
		var result = parser.parse( findPuResource( "multipu" ) );
		assertThat( result )
				.containsOnlyKeys( "multipu1", "multipu2", "multipu3" );
		assertThat( result.get( "multipu1" ) )
				.returns( "multipu1", PersistenceUnitDescriptor::getName )
				.returns(
						PersistenceUnitTransactionType.RESOURCE_LOCAL, PersistenceUnitDescriptor::getTransactionType );
		assertThat( result.get( "multipu2" ) )
				.returns( "multipu2", PersistenceUnitDescriptor::getName )
				.returns(
						PersistenceUnitTransactionType.RESOURCE_LOCAL, PersistenceUnitDescriptor::getTransactionType );
		assertThat( result.get( "multipu3" ) )
				.returns( "multipu3", PersistenceUnitDescriptor::getName )
				.returns( PersistenceUnitTransactionType.JTA, PersistenceUnitDescriptor::getTransactionType );
	}

	@Test
	public void parse_defaultTransactionType() {
		var parser = PersistenceXmlParser.create();
		var result = parser.parse( findPuResource( "multipu" ), PersistenceUnitTransactionType.JTA );
		assertThat( result.get( "multipu1" ) )
				.returns( "multipu1", PersistenceUnitDescriptor::getName )
				.returns(
						PersistenceUnitTransactionType.JTA, PersistenceUnitDescriptor::getTransactionType );
		assertThat( result.get( "multipu2" ) )
				.returns( "multipu2", PersistenceUnitDescriptor::getName )
				.returns( PersistenceUnitTransactionType.RESOURCE_LOCAL, PersistenceUnitDescriptor::getTransactionType );
		assertThat( result.get( "multipu3" ) )
				.returns( "multipu3", PersistenceUnitDescriptor::getName )
				.returns( PersistenceUnitTransactionType.JTA, PersistenceUnitDescriptor::getTransactionType );
	}

	private static URL findPuResource(String resourceName) {
		return findAsResource( "org/hibernate/jpa/test/persistenceunit/META-INF/" + resourceName + ".xml" );
	}

	private static class TestClassLoader extends ClassLoader {
		private final URL url;

		public TestClassLoader(String resourceName) {
			url = findPuResource( resourceName );
		}

		@Override
		protected Enumeration<URL> findResources(String name) throws IOException {
			return name.equals( "META-INF/persistence.xml" ) ?
					Collections.enumeration( List.of( url ) ) :
					Collections.emptyEnumeration();
		}
	}
}
