/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.idclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.test.util.SchemaUtil.getColumnNames;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test that bootstrap doesn't throw an exception
 * when an entity has a composite ID containing an association to another entity,
 * which itself has a composite ID containing an association to another entity.
 * <p>
 * This test used to fail on bootstrap with the following error:
 * <p>
 * org.hibernate.MappingException: identifier mapping has wrong number of columns: org.hibernate.test.idclass.IdClassForNestedIdWithAssociationTest$NestedIdClassEntity type: component[idClassEntity,key3]
 * 	at org.hibernate.mapping.RootClass.validate(RootClass.java:273)
 * 	at org.hibernate.boot.internal.MetadataImpl.validate(MetadataImpl.java:359)
 * 	at org.hibernate.internal.SessionFactoryImpl.<init>(SessionFactoryImpl.java:307)
 * 	at org.hibernate.boot.internal.SessionFactoryBuilderImpl.build(SessionFactoryBuilderImpl.java:471)
 * 	at org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase.buildResources(BaseNonConfigCoreFunctionalTestCase.java:165)
 * 	at org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase.startUp(BaseNonConfigCoreFunctionalTestCase.java:141)
 * 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
 * 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
 * 	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
 * 	at java.base/java.lang.reflect.Method.invoke(Method.java:566)
 * 	at org.hibernate.testing.junit4.TestClassMetadata.performCallbackInvocation(TestClassMetadata.java:205)
 */
@TestForIssue(jiraKey = "HHH-14918")
public class IdClassForNestedIdWithAssociationTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BasicEntity.class, IdClassEntity.class, NestedIdClassEntity.class };
	}

	@Test
	public void metadataTest() {
		assertThat( getColumnNames( "NestedIdClassEntity", metadata() ) )
				// Just check we're using copied IDs; otherwise the test wouldn't be able to reproduce HHH-14918.
				.containsExactlyInAnyOrder( "idClassEntity_basicEntity_key1", "idClassEntity_key2", "key3" );
	}

	// The main goal of the test is to check that bootstrap doesn't throw an exception,
	// but it feels wrong to have a test class with just an empty test method,
	// so just check that persisting/loading works correctly.
	@Test
	public void smokeTest() {
		inTransaction( s -> {
			BasicEntity basic = new BasicEntity( 1L );
			s.persist( basic );
			IdClassEntity idClass = new IdClassEntity( basic, 2L );
			s.persist( idClass );
			NestedIdClassEntity nestedIdClass = new NestedIdClassEntity( idClass, 3L );
			s.persist( nestedIdClass );
		} );

		inTransaction( s -> {
			NestedIdClassEntity nestedIdClass = s.get(
					NestedIdClassEntity.class,
					new NestedIdClassEntity.NestedIdClassEntityId( 1L, 2L, 3L )
			);
			assertThat( nestedIdClass )
					.extracting( NestedIdClassEntity::getKey3 )
					.isEqualTo( 3L );
			IdClassEntity idClass = nestedIdClass.getIdClassEntity();
			assertThat( idClass )
					.extracting( IdClassEntity::getKey2 )
					.isEqualTo( 2L );
			BasicEntity basic = idClass.basicEntity;
			assertThat( basic )
					.extracting( BasicEntity::getKey1 )
					.isEqualTo( 1L );
		} );
	}

	@Entity(name = "BasicEntity")
	public static class BasicEntity {
		@Id
		Long key1;

		protected BasicEntity() {
		}

		public BasicEntity(long key1) {
			this.key1 = key1;
		}

		public Long getKey1() {
			return key1;
		}
	}

	@Entity(name = "IdClassEntity")
	@IdClass(IdClassEntity.IdClassEntityId.class)
	public static class IdClassEntity {
		@Id
		@ManyToOne
		BasicEntity basicEntity;
		@Id
		Long key2;

		protected IdClassEntity() {
		}

		public IdClassEntity(BasicEntity basicEntity, long key2) {
			this.basicEntity = basicEntity;
			this.key2 = key2;
		}

		public BasicEntity getBasicEntity() {
			return basicEntity;
		}

		public Long getKey2() {
			return key2;
		}

		public static class IdClassEntityId implements Serializable {
			long basicEntity;
			long key2;

			protected IdClassEntityId() {
			}

			public IdClassEntityId(long basicEntity, long key2) {
				this.basicEntity = basicEntity;
				this.key2 = key2;
			}

			public long getBasicEntity() {
				return basicEntity;
			}

			public long getKey2() {
				return key2;
			}
		}
	}

	@Entity(name = "NestedIdClassEntity")
	@IdClass(NestedIdClassEntity.NestedIdClassEntityId.class)
	public static class NestedIdClassEntity {
		@Id
		@ManyToOne
		IdClassEntity idClassEntity;
		@Id
		Long key3;

		protected NestedIdClassEntity() {
		}

		public NestedIdClassEntity(IdClassEntity idClassEntity, long key3) {
			this.idClassEntity = idClassEntity;
			this.key3 = key3;
		}

		public IdClassEntity getIdClassEntity() {
			return idClassEntity;
		}

		public Long getKey3() {
			return key3;
		}

		public static class NestedIdClassEntityId implements Serializable {
			IdClassEntity.IdClassEntityId idClassEntity;
			long key3;

			protected NestedIdClassEntityId() {
			}

			public NestedIdClassEntityId(IdClassEntity.IdClassEntityId idClassEntity, long key3) {
				this.idClassEntity = idClassEntity;
				this.key3 = key3;
			}

			public NestedIdClassEntityId(long basicEntity, long key2, long key3) {
				this( new IdClassEntity.IdClassEntityId( basicEntity, key2 ), key3 );
			}
		}
	}
}
