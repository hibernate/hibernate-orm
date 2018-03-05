/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytomany;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12240")
public class MappedByEmbeddableAttributeTest extends BaseEnversJPAFunctionalTestCase {

	@Audited
	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		@Embedded
		private Container container;

		EntityA() {

		}

		EntityA(String name) {
			this( name, new Container() );
		}

		EntityA(String name, Container container) {
			this.name = name;
			this.container = container;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Container getContainer() {
			return container;
		}

		public void setContainer(Container container) {
			this.container = container;
		}
	}

	@Embeddable
	public static class Container {
		@ManyToMany
		private List<EntityB> bList = new ArrayList<>();

		public List<EntityB> getbList() {
			return bList;
		}

		public void setbList(List<EntityB> bList) {
			this.bList = bList;
		}
	}

	@Audited
	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		@ManyToMany(mappedBy = "container.bList")
		private List<EntityA> aList = new ArrayList<>();

		EntityB() {

		}

		EntityB(String name, EntityA... objects) {
			this.name = name;
			if ( objects.length > 0 ) {
				for ( EntityA a : objects ) {
					this.aList.add( a );
					a.getContainer().getbList().add( this );
				}
			}
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<EntityA> getaList() {
			return aList;
		}

		public void setaList(List<EntityA> aList) {
			this.aList = aList;
		}
	}
	
	private static class EntityBNameMatcher extends BaseMatcher<EntityB> {

		private final String expectedValue;

		public EntityBNameMatcher(String name) {
			this.expectedValue = name;
		}

		@Override
		public boolean matches(Object item) {
			if ( !( item instanceof EntityB ) ) {
				return false;
			}

			EntityB entityB = (EntityB) item;
			return Objects.equals( entityB.getName(), this.expectedValue );
		}

		@Override
		public void describeTo(Description description) {
			description.appendText( "an instance of " ).appendText( EntityB.class.getName() ).appendText( " named " ).appendValue( this.expectedValue );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class };
	}

	private Integer aId;
	private Integer bId1;
	private Integer bId2;

	@Test
	@Priority(10)
	public void initData() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final EntityA a = new EntityA( "A" );
			final EntityB b = new EntityB( "B", a );
			entityManager.persist( a );
			entityManager.persist( b );

			this.aId = a.getId();
			this.bId1 = b.getId();
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			final EntityA a = entityManager.find( EntityA.class, this.aId );
			for ( EntityB b : a.getContainer().getbList() ) {
				b.setName( b.getName() + "-Updated" );
				entityManager.merge( b );
			}
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			final EntityA a = entityManager.find( EntityA.class, this.aId );
			final EntityB b = new EntityB( "B2", a );
			entityManager.persist( b );
			entityManager.merge( a );

			this.bId2 = b.getId();
		} );
	}

	@Test
	public void testRevisionHistoryEntityA() {
		List<Number> aRevisions = getAuditReader().getRevisions( EntityA.class, this.aId );
		assertEquals( Arrays.asList( 1, 3 ), aRevisions );

		EntityA rev1 = getAuditReader().find( EntityA.class, this.aId, 1 );
		assertEquals( 1, rev1.getContainer().getbList().size() );
		assertEquals( "B", rev1.getContainer().getbList().get( 0 ).getName() );

		EntityA rev3 = getAuditReader().find( EntityA.class, this.aId, 3 );
		assertEquals( 2, rev3.getContainer().getbList().size() );
		assertThat( rev3.getContainer().getbList(), hasItem( new EntityBNameMatcher( "B-Updated" ) ) );
		assertThat( rev3.getContainer().getbList(), hasItem( new EntityBNameMatcher( "B2" ) ) );

	}

	@Test
	public void testRevisionHistoryEntityB() {
		List<Number> b1Revisions = getAuditReader().getRevisions( EntityB.class, this.bId1 );
		assertEquals( Arrays.asList( 1, 2 ), b1Revisions );

		EntityB b1Rev1 = getAuditReader().find( EntityB.class, this.bId1, 1 );
		assertEquals( "B", b1Rev1.getName() );
		assertEquals( 1, b1Rev1.getaList().size() );
		assertEquals( this.aId, b1Rev1.getaList().get( 0 ).getId() );

		EntityB b1Rev2 = getAuditReader().find( EntityB.class, this.bId1, 2 );
		assertEquals( "B-Updated", b1Rev2.getName() );
		assertEquals( 1, b1Rev1.getaList().size() );
		assertEquals( this.aId, b1Rev1.getaList().get( 0 ).getId() );

		List<Number> b2Revisions = getAuditReader().getRevisions( EntityB.class, this.bId2 );
		assertEquals( Arrays.asList( 3 ), b2Revisions );

		EntityB b2Rev3 = getAuditReader().find( EntityB.class, this.bId2, 3 );
		assertEquals( "B2", b2Rev3.getName() );
		assertEquals( 1, b2Rev3.getaList().size() );
		assertEquals( this.aId, b2Rev3.getaList().get( 0 ).getId() );
	}
}
