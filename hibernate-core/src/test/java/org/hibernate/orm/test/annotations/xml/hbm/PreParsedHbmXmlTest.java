/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.xml.hbm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.spi.Binding;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey(value = "HHH-14530")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
public class PreParsedHbmXmlTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		try (InputStream xmlStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream( "org/hibernate/orm/test/annotations/xml/hbm/pre-parsed-hbm.xml" )) {
			Binding<?> parsed = metadataSources.getXmlMappingBinderAccess().bind( xmlStream );
			metadataSources.addXmlBinding( parsed );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Test
	public void testPreParsedHbmXml() {
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
