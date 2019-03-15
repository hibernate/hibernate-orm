/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany;

import java.util.ArrayList;
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
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12240")
@Disabled("NYI - Support for nested mappedBy='someEmbeddable.someOtherAttribute'")
public class MappedByEmbeddableAttributeTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class };
	}

	private Integer aId;
	private Integer bId1;
	private Integer bId2;

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				entityManager -> {
					final EntityA a = new EntityA( "A" );
					final EntityB b = new EntityB( "B", a );
					entityManager.persist( a );
					entityManager.persist( b );

					this.aId = a.getId();
					this.bId1 = b.getId();
				},

				entityManager -> {
					final EntityA a = entityManager.find( EntityA.class, this.aId );
					for ( EntityB b : a.getContainer().getbList() ) {
						b.setName( b.getName() + "-Updated" );
						entityManager.merge( b );
					}
				},

				entityManager -> {
					final EntityA a = entityManager.find( EntityA.class, this.aId );
					final EntityB b = new EntityB( "B2", a );
					entityManager.persist( b );
					entityManager.merge( a );

					this.bId2 = b.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionHistoryEntityA() {
		assertThat( getAuditReader().getRevisions( EntityA.class, this.aId ), contains( 1, 3 ) );

		final EntityA rev1 = getAuditReader().find( EntityA.class, this.aId, 1 );
		assertThat( rev1.getContainer().getbList(), contains( new EntityBNameMatcher( "B" ) ) );

		final EntityA rev3 = getAuditReader().find( EntityA.class, this.aId, 3 );
		assertThat( rev3.getContainer().getbList(), contains( new EntityBNameMatcher( "B-Updated" ), new EntityBNameMatcher( "B2" ) ) );
	}

	@DynamicTest
	public void testRevisionHistoryEntityB() {
		assertThat( getAuditReader().getRevisions( EntityB.class, bId1 ), contains( 1, 2 ) );

		EntityB b1Rev1 = getAuditReader().find( EntityB.class, this.bId1, 1 );
		assertThat( b1Rev1.getName(), equalTo( "B" ) );
		assertThat( b1Rev1.getaList(), contains( new EntityAIdMatcher( aId ) ) );

		EntityB b1Rev2 = getAuditReader().find( EntityB.class, this.bId1, 2 );
		assertThat( b1Rev2.getName(), equalTo( "B-Updated" ) );
		assertThat( b1Rev2.getaList(), contains( new EntityAIdMatcher( aId ) ) );

		assertThat( getAuditReader().getRevisions( EntityB.class, bId2 ), contains( 3 ) );

		EntityB b2Rev3 = getAuditReader().find( EntityB.class, this.bId2, 3 );
		assertThat( b2Rev3.getName(), equalTo( "B2" ) );
		assertThat( b2Rev3.getaList(), contains( new EntityAIdMatcher( aId ) ) );
	}

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
			description.appendText( "an instance of " )
					.appendText( EntityB.class.getName() )
					.appendText( " named " )
					.appendValue( this.expectedValue );
		}
	}

	private static class EntityAIdMatcher extends BaseMatcher<EntityA> {
		private final Integer expectedValue;

		public EntityAIdMatcher(Integer value) {
			this.expectedValue = value;
		}

		@Override
		public boolean matches(Object item) {
			if ( !( item instanceof EntityA ) ) {
				return false;
			}

			EntityA entityA = (EntityA) item;
			return Objects.equals( entityA.getId(), this.expectedValue );
		}

		@Override
		public void describeTo(Description description) {
			description.appendText( "an instance of " )
					.appendText( EntityA.class.getName() )
					.appendText( " with id " )
					.appendValue( this.expectedValue );
		}
	}
}
