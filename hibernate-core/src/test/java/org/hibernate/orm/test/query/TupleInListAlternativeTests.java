/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.*;

import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

@DomainModel(annotatedClasses = {
		TupleInListAlternativeTests.EntityWithEmbeddedSubEntity.class
})
@ServiceRegistry
@SessionFactory
public class TupleInListAlternativeTests {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityWithEmbeddedSubEntity entity = new EntityWithEmbeddedSubEntity();
					entity.setId( 1 );
					entity.setSub( new EntityWithEmbeddedSubEntity.Sub( "ZH2", "HDS" ) );
					entity.setPresent( "Y" );
					session.persist( entity );
					EntityWithEmbeddedSubEntity entity2 = new EntityWithEmbeddedSubEntity();
					entity2.setId( 2 );
					entity2.setSub( new EntityWithEmbeddedSubEntity.Sub( "ZH3", "HDS" ) );
					entity2.setPresent( "Y" );
					session.persist( entity2 );
					EntityWithEmbeddedSubEntity entity3 = new EntityWithEmbeddedSubEntity();
					entity3.setId( 3 );
					entity3.setSub( new EntityWithEmbeddedSubEntity.Sub( "GXZ", "HDS" ) );
					entity3.setPresent( "N" );
					session.persist( entity3 );
					EntityWithEmbeddedSubEntity entity4 = new EntityWithEmbeddedSubEntity();
					entity4.setId( 4 );
					entity4.setSub( new EntityWithEmbeddedSubEntity.Sub( "KAZ", "TST" ) );
					entity4.setPresent( "Y" );
					session.persist( entity4 );
				} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-16886")
	public void testQueryResultsForHhh16886(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<EntityWithEmbeddedSubEntity> resultList = session
							.createQuery(
									"select e from EntityWithEmbeddedSubEntity e where e.sub in :subs and e.present = :present",
									EntityWithEmbeddedSubEntity.class
							)
							.setParameter(
									"subs",
									Arrays.asList( new EntityWithEmbeddedSubEntity.Sub( "ZH2", "HDS" ),
												new EntityWithEmbeddedSubEntity.Sub( "GXZ", "HDS" ),
												new EntityWithEmbeddedSubEntity.Sub( "KAZ", "TST" )
									)
							)
							.setParameter( "present", "Y" ).list();
					assertThat( resultList.size(), equalTo( 2 ) );
					assertThat( resultList.stream()
										.map( EntityWithEmbeddedSubEntity::getId )
										.collect( Collectors.toList() ), hasItems( 1, 4 ) );
					assertThat( resultList.stream()
										.map( EntityWithEmbeddedSubEntity::getId )
										.collect( Collectors.toList() ), not( hasItems( 2, 3 ) ) );
				} );
	}

	@Entity(name = "EntityWithEmbeddedSubEntity")
	public static class EntityWithEmbeddedSubEntity {

		@Id
		@Column(name = "id")
		private int id;

		@Embedded
		private Sub sub;

		@Column(name = "present")
		private String present;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Sub getSub() {
			return sub;
		}

		public void setSub(Sub sub) {
			this.sub = sub;
		}

		public String getPresent() {
			return present;
		}

		public void setPresent(String present) {
			this.present = present;
		}

		@Embeddable
		public static class Sub {

			public Sub() {
			}

			public Sub(String key1, String key2) {
				this.setKey1( key1 );
				this.setKey2( key2 );
			}

			@Column(name = "key_1")
			private String key1;

			@Column(name = "key_2")
			private String key2;

			public String getKey1() {
				return key1;
			}

			public void setKey1(String key1) {
				this.key1 = key1;
			}

			public String getKey2() {
				return key2;
			}

			public void setKey2(String key2) {
				this.key2 = key2;
			}
		}
	}
}
