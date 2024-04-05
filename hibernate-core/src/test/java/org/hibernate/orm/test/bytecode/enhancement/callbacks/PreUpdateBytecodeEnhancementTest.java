/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.callbacks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;


@JiraKey("HHH-12718")
@DomainModel(
		annotatedClasses = {
				PreUpdateBytecodeEnhancementTest.Person.class
		}
)
@ServiceRegistry(
		settings = {
				// TODO: how to convert this, or even is it needed?
				// options.put( AvailableSettings.CLASSLOADERS, getClass().getClassLoader() );
				@Setting( name = AvailableSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION, value = "true" ),
				@Setting( name = AvailableSettings.ENHANCER_ENABLE_DIRTY_TRACKING, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class PreUpdateBytecodeEnhancementTest {

	@Test
	public void testPreUpdateModifications(SessionFactoryScope scope) {
		Person person = new Person();

		scope.inTransaction( entityManager -> {
			entityManager.persist( person );
		} );

		scope.inTransaction( entityManager -> {
			Person p = entityManager.find( Person.class, person.id );
			assertNotNull( p );
			assertNotNull( p.createdAt );
			assertNull( p.lastUpdatedAt );

			p.setName( "Changed Name" );
		} );

		scope.inTransaction( entityManager -> {
			Person p = entityManager.find( Person.class, person.id );
			assertNotNull( p.lastUpdatedAt );
		} );
	}

	@Entity(name = "Person")
	static class Person {
		@Id
		@GeneratedValue
		private int id;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		private Instant createdAt;

		public Instant getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(Instant createdAt) {
			this.createdAt = createdAt;
		}

		private Instant lastUpdatedAt;

		public Instant getLastUpdatedAt() {
			return lastUpdatedAt;
		}

		public void setLastUpdatedAt(Instant lastUpdatedAt) {
			this.lastUpdatedAt = lastUpdatedAt;
		}

		@ElementCollection
		private List<String> tags;

		@Lob
		@Basic(fetch = FetchType.LAZY)
		private byte[] image;

		@PrePersist
		void beforeCreate() {
			this.setCreatedAt( Instant.now() );
		}

		@PreUpdate
		void beforeUpdate() {
			this.setLastUpdatedAt( Instant.now() );
		}
	}
}
