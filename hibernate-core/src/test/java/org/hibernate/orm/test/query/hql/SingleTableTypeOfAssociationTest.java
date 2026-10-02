package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The type of a self-referencing association must use the associated entity's discriminator.
///
/// @author Steve Ebersole
@JiraKey( "HHH-14402" )
@DomainModel( annotatedClasses = {
		SingleTableTypeOfAssociationTest.GeographicPlace.class,
		SingleTableTypeOfAssociationTest.SupervisedTerritory.class,
		SingleTableTypeOfAssociationTest.OtherPlace.class
} )
@SessionFactory
public class SingleTableTypeOfAssociationTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SupervisedTerritory territory = new SupervisedTerritory( 1L, "territory", null );
			final OtherPlace other = new OtherPlace( 2L, "other", null );
			session.persist( territory );
			session.persist( other );
			session.persist( new OtherPlace( 3L, "matching-parent", territory ) );
			session.persist( new SupervisedTerritory( 4L, "nonmatching-parent", other ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testParentTypeMatchesDifferentChildType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final GeographicPlace result = session.createQuery(
					"from GeographicPlace where nameNormalized = :name and type(parent) = SupervisedTerritory",
					GeographicPlace.class
			)
					.setParameter( "name", "matching-parent" )
					.setMaxResults( 1 )
					.getSingleResult();
			assertEquals( 3L, result.id );
		} );
	}

	@Test
	public void testChildTypeDoesNotSubstituteForParentType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertTrue( session.createQuery(
					"from GeographicPlace where nameNormalized = :name and type(parent) = SupervisedTerritory",
					GeographicPlace.class
			)
					.setParameter( "name", "nonmatching-parent" )
					.setMaxResults( 1 )
					.getResultList().isEmpty() );
		} );
	}

	@Entity( name = "GeographicPlace" )
	@Table( name = "hhh14402_place" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "ENTITY_ID", discriminatorType = DiscriminatorType.INTEGER )
	public abstract static class GeographicPlace {
		@Id
		private Long id;

		private String nameNormalized;

		@ManyToOne
		private GeographicPlace parent;

		protected GeographicPlace() {
		}

		protected GeographicPlace(Long id, String name, GeographicPlace parent) {
			this.id = id;
			this.nameNormalized = name;
			this.parent = parent;
		}
	}

	@Entity( name = "SupervisedTerritory" )
	@DiscriminatorValue( "203" )
	public static class SupervisedTerritory extends GeographicPlace {
		public SupervisedTerritory() {
		}

		public SupervisedTerritory(Long id, String name, GeographicPlace parent) {
			super( id, name, parent );
		}
	}

	@Entity( name = "OtherPlace" )
	@DiscriminatorValue( "204" )
	public static class OtherPlace extends GeographicPlace {
		public OtherPlace() {
		}

		public OtherPlace(Long id, String name, GeographicPlace parent) {
			super( id, name, parent );
		}
	}
}
