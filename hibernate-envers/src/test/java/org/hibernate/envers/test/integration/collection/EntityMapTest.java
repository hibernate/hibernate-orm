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
import javax.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * Tests an entity mapping using an entity as a map-key and map-value.
 *
 * This only fails on {@code DefaultAuditStrategy} because the {@code ValidityAuditStrategy} does
 * not make use of the related-id data of the middle table like the default audit strategy.
 *
 * This test verifies both strategies work, but the failure is only applicable for the default strategy.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11892")
public class EntityMapTest extends BaseEnversJPAFunctionalTestCase {

	private A a;
	private B b1;
	private B b2;
	private C c1;
	private C c2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { A.class, B.class, C.class };
	}

	@MappedSuperclass
	public static abstract class AbstractEntity {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			AbstractEntity that = (AbstractEntity) o;

			return id != null ? id.equals( that.id ) : that.id == null;
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}
	}

	@Entity(name = "A")
	@Audited
	public static class A extends AbstractEntity {
		@ElementCollection
		private Map<B, C> map = new HashMap<>();

		public Map<B, C> getMap() {
			return map;
		}

		public void setMap(Map<B, C> map) {
			this.map = map;
		}
	}

	@Entity(name = "B")
	@Audited
	public static class B extends AbstractEntity {

	}

	@Entity(name = "C")
	@Audited
	public static class C extends AbstractEntity {

	}

	@Test
	@Priority(10)
	public void initData() {
		// add b/c key-pair to the map and save a entity.
		doInJPA( this::entityManagerFactory, entityManager -> {
			final A a = new A();

			final B b = new B();
			final C c = new C();
			entityManager.persist( b );
			entityManager.persist( c );

			a.getMap().put( b, c );
			entityManager.persist( a );

			this.a = a;
			this.b1 = b;
			this.c1 = c;
		} );

		// add a new b/c key-pair to the map
		doInJPA( this::entityManagerFactory, entityManager -> {
			final A a = entityManager.find( A.class, this.a.getId() );

			final B b = new B();
			final C c = new C();
			entityManager.persist( b );
			entityManager.persist( c );

			a.getMap().put( b, c );
			entityManager.merge( a );

			this.b2 = b;
			this.c2 = c;
		} );

		// Remove b1 from the map
		doInJPA( this::entityManagerFactory, entityManager -> {
			final A a = entityManager.find( A.class, this.a.getId() );
			a.getMap().remove( this.b1 );
			entityManager.merge( a );
		} );
	}

	@Test
	public void testRevisionHistory() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			assertEquals( Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions( A.class, a.getId() ) );

			assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( B.class, b1.getId() ) );
			assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( C.class, c1.getId() ) );

			assertEquals( Arrays.asList( 2 ), getAuditReader().getRevisions( B.class, b2.getId() ) );
			assertEquals( Arrays.asList( 2 ), getAuditReader().getRevisions( C.class, c2.getId() ) );
		} );
	}

	@Test
	public void testRevision1() {
		final A rev1 = getAuditReader().find( A.class, this.a.getId(), 1 );
		assertEquals( 1, rev1.getMap().size() );
		assertEquals( TestTools.makeMap( this.b1, this.c1 ), rev1.getMap() );
	}

	@Test
	public void testRevision2() {
		final A rev2 = getAuditReader().find( A.class, this.a.getId(), 2 );
		assertEquals( 2, rev2.getMap().size() );
		assertEquals( TestTools.makeMap( this.b1, this.c1, this.b2, this.c2 ), rev2.getMap() );
	}

	@Test
	public void testRevision3() {
		final A rev3 = getAuditReader().find( A.class, this.a.getId(), 3 );
		assertEquals( 1, rev3.getMap().size() );
		assertEquals( TestTools.makeMap( this.b2, this.c2 ), rev3.getMap() );
	}

}
