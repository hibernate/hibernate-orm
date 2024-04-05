package org.hibernate.orm.test.bytecode.enhancement.update;

import java.util.List;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DomainModel(
		annotatedClasses = {
				JoinedInheritanceTest.Plane.class, JoinedInheritanceTest.A320.class
		}
)
@SessionFactory
@BytecodeEnhanced
@JiraKey("HHH-17632")
public class JoinedInheritanceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					A320 a320 = new A320( 1l, "Airbus A320", 101, true, "1.0" );
					session.persist( a320 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from A320" ).executeUpdate();
				}
		);
	}

	@Test
	public void testUpdateField(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					A320 referenceById = session.getReference( A320.class, 1L );

					referenceById.setSoftwareVersion( "2.0" );

					session.flush();
					session.clear();

					List<A320> airbusA320 = session.createQuery(
							"select a from A320 a where a.description = 'Airbus A320'" ).getResultList();
					assertFalse( airbusA320.isEmpty() );
					assertEquals( 1, airbusA320.size() );
					A320 a320 = airbusA320.get( 0 );
					assertEquals( "2.0", a320.getSoftwareVersion() );
					assertTrue( a320.getLarge() );
				}
		);
	}

	@Test
	public void testUpdateTwoFields(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					A320 referenceById = session.getReference( A320.class, 1L );

					referenceById.setSoftwareVersion( "2.0" );
					referenceById.setNbrOfSeats( 103 );

					session.flush();
					session.clear();

					List<A320> airbusA320 = session.createQuery(
							"select a from A320 a where a.description like 'Airbus A320'" ).getResultList();
					assertFalse( airbusA320.isEmpty() );
					assertEquals( 1, airbusA320.size() );
					A320 a320 = airbusA320.get( 0 );
					assertEquals( "2.0", a320.getSoftwareVersion() );
					assertEquals( 103, a320.getNbrOfSeats() );
					assertTrue( a320.getLarge() );
				}
		);
	}


	@Entity(name = "A320")
	@Table(name = "a320")
	public static class A320 extends Plane {

		@Column(name = "software_version")
		private String softwareVersion;

		public A320() {
			super();
		}

		public A320(Long id, String description, int nbrOfSeats, Boolean large, String softwareVersion) {
			super( id, description, nbrOfSeats, large );
			this.softwareVersion = softwareVersion;
		}

		public String getSoftwareVersion() {
			return softwareVersion;
		}

		public void setSoftwareVersion(String softwareVersion) {
			this.softwareVersion = softwareVersion;
		}
	}

	@Table(name = "plane_table")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn
	@Entity(name = "Plane")
	public static class Plane {

		@Id
		private Long id;

		@Column
		private String description;

		@Column(name = "nbr_of_seats")
		private int nbrOfSeats;

		@Basic(fetch = LAZY)
		private Boolean large = false;

		public Plane() {
		}

		public Plane(Long id, String description, int nbrOfSeats, Boolean large) {
			this.id = id;
			this.description = description;
			this.nbrOfSeats = nbrOfSeats;
			this.large = large;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public int getNbrOfSeats() {
			return nbrOfSeats;
		}

		public void setNbrOfSeats(int nbrOfSeats) {
			this.nbrOfSeats = nbrOfSeats;
		}

		public Boolean getLarge() {
			return large;
		}

		public void setLarge(Boolean large) {
			this.large = large;
		}
	}
}
