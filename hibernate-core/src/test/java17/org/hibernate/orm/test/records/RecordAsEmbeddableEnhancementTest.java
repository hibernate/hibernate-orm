/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.records;


import org.hibernate.engine.spi.ManagedComposite;
import org.hibernate.engine.spi.ManagedEntity;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@JiraKey( "HHH-15072" )
@DomainModel(
		annotatedClasses = {
				RecordAsEmbeddableEnhancementTest.MyEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true, extendedEnhancement = true, inlineDirtyChecking = true)
public class RecordAsEmbeddableEnhancementTest {

	@Test
	public void test(SessionFactoryScope scope) {
		// Ensure entity is enhanced, but not the record class
		assertTrue( ManagedEntity.class.isAssignableFrom( MyEntity.class ) );
		assertFalse( ManagedComposite.class.isAssignableFrom( MyRecord.class ) );

		scope.inTransaction(
				session -> {
					session.persist( new MyEntity( 1L, new MyRecord( "test", "abc" ) ) );
				}
		);

		scope.inTransaction(
				session -> {
					MyEntity myEntity = session.get( MyEntity.class, 1L );
					assertNotNull( myEntity );
					assertEquals( "test", myEntity.getRecord().name() );
					assertEquals( "abc", myEntity.getRecord().description() );

					myEntity.setRecord( new MyRecord( "test2", "def" ) );
				}
		);
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		Long id;
		@Embedded
		MyRecord record;

		public MyEntity() {
		}

		public MyEntity(Long id, MyRecord record) {
			this.id = id;
			this.record = record;
		}

		public Long getId() {
			return id;
		}

		public MyRecord getRecord() {
			return record;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setRecord(MyRecord record) {
			this.record = record;
		}
	}

	@Embeddable
	public static record MyRecord(String name, String description) {}
}
