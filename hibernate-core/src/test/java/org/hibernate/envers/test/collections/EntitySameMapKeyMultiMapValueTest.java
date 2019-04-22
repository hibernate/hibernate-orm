/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12018")
@Disabled("Attempts to instantiate Status enum via ManagedBeanRegistry throwing unable to locate no-arg constructor for bean.")
public class EntitySameMapKeyMultiMapValueTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	private Integer otherEntityId;
	private Integer someEntityId;

	@Entity(name = "SomeEntity")
	@Audited
	public static class SomeEntity {
		@Id
		@GeneratedValue
		private Integer id;

		@ElementCollection
		private Map<OtherEntity, Status> map = new HashMap<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Map<OtherEntity, Status> getMap() {
			return map;
		}

		public void setMap(Map<OtherEntity, Status> map) {
			this.map = map;
		}

		enum Status {
			A, B
		};
	}

	@Entity(name = "OtherEntity")
	@Audited
	public static class OtherEntity {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SomeEntity.class, OtherEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					final SomeEntity someEntity = new SomeEntity();
					final OtherEntity otherEntity = new OtherEntity();
					entityManager.persist( otherEntity );

					someEntity.getMap().put( otherEntity, SomeEntity.Status.A );
					entityManager.persist( someEntity );

					this.otherEntityId = otherEntity.getId();
					this.someEntityId = someEntity.getId();
				}
		);

		inTransaction(
				entityManager -> {
					final SomeEntity someEntity = entityManager.find( SomeEntity.class, someEntityId );
					final OtherEntity otherEntity = entityManager.find( OtherEntity.class, otherEntityId );
					someEntity.getMap().put( otherEntity, SomeEntity.Status.B );
					entityManager.merge( someEntity );
				}
		);

		inTransaction(
				entityManager -> {
					final SomeEntity someEntity = entityManager.find( SomeEntity.class, someEntityId );
					someEntity.getMap().clear();
					entityManager.merge( someEntity );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( OtherEntity.class, otherEntityId ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( SomeEntity.class, someEntityId ), contains( 1, 2, 3 ) );
	}

	@DynamicTest
	public void testRevisionOne() {
		final SomeEntity someEntity = getAuditReader().find( SomeEntity.class, someEntityId, 1 );
		assertThat( someEntity, notNullValue() );
		assertThat( someEntity.getMap().entrySet(), CollectionMatchers.hasSize( 1 ) );

		final OtherEntity otherEntity = getAuditReader().find( OtherEntity.class, otherEntityId, 1 );
		assertNotNull( otherEntity );

		final Map.Entry<OtherEntity, SomeEntity.Status> entry = someEntity.getMap().entrySet().iterator().next();
		assertThat( entry.getKey(), equalTo( otherEntity ) );
		assertThat( entry.getValue(), equalTo( SomeEntity.Status.A ) );
	}

	@DynamicTest
	public void testRevisionTwo() {
		final SomeEntity someEntity = getAuditReader().find( SomeEntity.class, someEntityId, 2 );
		assertThat( someEntity, notNullValue() );
		assertThat( someEntity.getMap().entrySet(), CollectionMatchers.hasSize( 1 ) );

		final OtherEntity otherEntity = getAuditReader().find( OtherEntity.class, otherEntityId, 2 );
		assertThat( otherEntity, notNullValue() );

		final Map.Entry<OtherEntity, SomeEntity.Status> entry = someEntity.getMap().entrySet().iterator().next();
		assertThat( entry.getKey(), equalTo( otherEntity ) );
		assertThat( entry.getValue(), equalTo( SomeEntity.Status.B ) );
	}

	@DynamicTest
	public void testRevisionThree() {
		final SomeEntity someEntity = getAuditReader().find( SomeEntity.class, someEntityId, 3 );
		assertThat( someEntity, notNullValue() );
		assertThat( someEntity.getMap().entrySet(), CollectionMatchers.isEmpty() );
	}
}
