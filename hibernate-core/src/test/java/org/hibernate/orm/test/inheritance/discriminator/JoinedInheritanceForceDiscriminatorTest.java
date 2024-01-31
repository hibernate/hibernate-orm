/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.discriminator;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.DiscriminatorOptions;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andrea Boriero
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		JoinedInheritanceForceDiscriminatorTest.CommonBase.class,
		JoinedInheritanceForceDiscriminatorTest.ElementEntity.class,
		JoinedInheritanceForceDiscriminatorTest.AnotherEntity.class,
		JoinedInheritanceForceDiscriminatorTest.ElementGroup.class
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17113" )
public class JoinedInheritanceForceDiscriminatorTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ElementEntity element = new ElementEntity( 1L, "element_1" );
			session.persist( element );
			final AnotherEntity another = new AnotherEntity( 2L, "another_2" );
			session.persist( another );
			final ElementGroup elementGroup = new ElementGroup( 3L );
			elementGroup.addElement( element );
			session.persist( elementGroup );
		} );
		scope.inTransaction( session -> {
			// Emulate association with AnotherEntity on the same element_table
			session.createNativeMutationQuery( "update element_table set group_id = 3 where id = 2" ).executeUpdate();
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from CommonBase" ).executeUpdate();
			session.createMutationQuery( "delete from ElementGroup" ).executeUpdate();
		} );
	}

	@Test
	public void testFindAndLoad(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final ElementGroup group = session.find( ElementGroup.class, 3L );
			inspector.clear();
			final List<ElementEntity> elements = group.getElements();
			assertThat( Hibernate.isInitialized( elements ) ).isFalse();
			assertThat( elements ).hasSize( 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
			final CommonBase commonBase = elements.get( 0 );
			assertThat( commonBase.getId() ).isEqualTo( 1L );
			assertThat( commonBase.getName() ).isEqualTo( "element_1" );
		} );
	}

	@Test
	public void testQueryAndLoad(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final ElementGroup group = session.createQuery(
					"from ElementGroup",
					ElementGroup.class
			).getSingleResult();
			inspector.clear();
			final List<ElementEntity> elements = group.getElements();
			assertThat( Hibernate.isInitialized( elements ) ).isFalse();
			assertThat( elements ).hasSize( 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
			final CommonBase commonBase = elements.get( 0 );
			assertThat( commonBase.getId() ).isEqualTo( 1L );
			assertThat( commonBase.getName() ).isEqualTo( "element_1" );
		} );
	}

	@Test
	public void testQueryAndJoinFetch(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final ElementGroup group = session.createQuery(
					"from ElementGroup g join fetch g.elements",
					ElementGroup.class
			).getSingleResult();
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
			final List<ElementEntity> elements = group.getElements();
			assertThat( Hibernate.isInitialized( elements ) ).isTrue();
			assertThat( elements ).hasSize( 1 );
			final CommonBase commonBase = elements.get( 0 );
			assertThat( commonBase.getId() ).isEqualTo( 1L );
			assertThat( commonBase.getName() ).isEqualTo( "element_1" );
		} );
	}

	@Inheritance( strategy = InheritanceType.JOINED )
	@DiscriminatorColumn( name = "disc_col" )
	@DiscriminatorOptions( force = true )
	@Entity( name = "CommonBase" )
	public static class CommonBase {
		@Id
		protected Long id;

		protected String name;

		public CommonBase() {
		}

		public CommonBase(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "ElementEntity" )
	@DiscriminatorValue( "element" )
	@Table( name = "element_table" )
	public static class ElementEntity extends CommonBase {
		public ElementEntity() {
		}

		public ElementEntity(Long id, String name) {
			super( id, name );
		}
	}

	@Entity( name = "AnotherEntity" )
	@DiscriminatorValue( "another" )
	@Table( name = "element_table" )
	public static class AnotherEntity extends CommonBase {
		public AnotherEntity() {
		}

		public AnotherEntity(Long id, String name) {
			super( id, name );
		}
	}

	@Entity( name = "ElementGroup" )
	public static class ElementGroup {
		@Id
		protected Long id;

		@OneToMany( fetch = FetchType.LAZY )
		@JoinColumn( name = "group_id" )
		private List<ElementEntity> elements = new ArrayList<>();

		public ElementGroup() {
		}

		public ElementGroup(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public List<ElementEntity> getElements() {
			return elements;
		}

		public void addElement(ElementEntity element) {
			elements.add( element );
		}
	}
}