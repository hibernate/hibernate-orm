/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.xml.ejb3;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@TestForIssue(jiraKey = {"HHH-14530", "HHH-14529"})
public class PreParsedOrmXmlTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		super.prepareBootstrapRegistryBuilder( builder );
	}

	@Override
	protected void addMappings(Configuration configuration) {
		super.addMappings( configuration );
		try (InputStream xmlStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream( "org/hibernate/test/annotations/xml/ejb3/pre-parsed-orm.xml" )) {
			Binding<?> parsed = configuration.getXmlMappingBinderAccess().bind( xmlStream );
			configuration.addXmlMapping( parsed );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Test
	public void testPreParsedOrmXml() {
		// Just check that the entity can be persisted, which means the mapping file was taken into account
		NonAnnotatedEntity persistedEntity = new NonAnnotatedEntity( "someName" );
		inTransaction( s -> s.persist( persistedEntity ) );
		inTransaction( s -> {
			NonAnnotatedEntity retrievedEntity = s.find( NonAnnotatedEntity.class, persistedEntity.getId() );
			assertThat( retrievedEntity ).extracting( NonAnnotatedEntity::getName )
					.isEqualTo( persistedEntity.getName() );
		} );
	}

	public static class NonAnnotatedEntity {
		private long id;

		private String name;

		public NonAnnotatedEntity() {
		}

		public NonAnnotatedEntity(String name) {
			this.name = name;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
