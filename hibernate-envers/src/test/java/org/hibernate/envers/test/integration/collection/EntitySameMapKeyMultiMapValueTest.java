/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12018")
public class EntitySameMapKeyMultiMapValueTest extends BaseEnversJPAFunctionalTestCase {

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

	@Test
	@Priority(10)
	public void initData() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final SomeEntity someEntity = new SomeEntity();
			final OtherEntity otherEntity = new OtherEntity();
			entityManager.persist( otherEntity );

			someEntity.getMap().put( otherEntity, SomeEntity.Status.A );
			entityManager.persist( someEntity );

			this.otherEntityId = otherEntity.getId();
			this.someEntityId = someEntity.getId();

		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			final SomeEntity someEntity = entityManager.find( SomeEntity.class, someEntityId );
			final OtherEntity otherEntity = entityManager.find( OtherEntity.class, otherEntityId );
			someEntity.getMap().put( otherEntity, SomeEntity.Status.B );
			entityManager.merge( someEntity );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			final SomeEntity someEntity = entityManager.find( SomeEntity.class, someEntityId );
			someEntity.getMap().clear();
			entityManager.merge( someEntity );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( OtherEntity.class, otherEntityId ) );
		assertEquals( Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions( SomeEntity.class, someEntityId ) );
	}

	@Test
	public void blockTest() {
		final AuditReader reader = getAuditReader();
		System.out.println( "Halt" );
	}

	@Test
	public void testRevisionOne() {
		final SomeEntity someEntity = getAuditReader().find( SomeEntity.class, someEntityId, 1 );
		assertNotNull( someEntity );
		assertFalse( someEntity.getMap().isEmpty() );
		assertEquals( 1, someEntity.getMap().size() );

		final OtherEntity otherEntity = getAuditReader().find( OtherEntity.class, otherEntityId, 1 );
		assertNotNull( otherEntity );

		final Map.Entry<OtherEntity, SomeEntity.Status> entry = someEntity.getMap().entrySet().iterator().next();
		assertEquals( otherEntity, entry.getKey() );
		assertEquals( SomeEntity.Status.A, entry.getValue() );
	}

	@Test
	public void testRevisionTwo() {
		final SomeEntity someEntity = getAuditReader().find( SomeEntity.class, someEntityId, 2 );
		assertNotNull( someEntity );
		assertFalse( someEntity.getMap().isEmpty() );
		assertEquals( 1, someEntity.getMap().size() );

		final OtherEntity otherEntity = getAuditReader().find( OtherEntity.class, otherEntityId, 2 );
		assertNotNull( otherEntity );

		final Map.Entry<OtherEntity, SomeEntity.Status> entry = someEntity.getMap().entrySet().iterator().next();
		assertEquals( otherEntity, entry.getKey() );
		assertEquals( SomeEntity.Status.B, entry.getValue() );
	}

	@Test
	public void testRevisionThree() {
		final SomeEntity someEntity = getAuditReader().find( SomeEntity.class, someEntityId, 3 );
		assertNotNull( someEntity );
		assertTrue( someEntity.getMap().isEmpty() );
	}
}
