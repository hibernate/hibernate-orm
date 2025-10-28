/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey( "HHH-11591" )
@DomainModel(
		annotatedClasses = { OneToOneNotFoundTest.Show.class, OneToOneNotFoundTest.ShowDescription.class }
)
@SessionFactory
public class OneToOneNotFoundTest {

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// Show#1 will end up with a dangling foreign-key as the
			// matching row on the Description table is deleted
			{
				Show show = new Show( 1, new ShowDescription( 10 ) );
				session.persist( show );

			}

			// Show#2 will end up with a dangling foreign-key as the
			// matching row on the join-table is deleted
			{
				Show show = new Show( 2, new ShowDescription( 20 ) );
				session.persist( show );
			}

			// Show#3 will end up as an inverse dangling foreign-key from
			// Description because the matching row is deleted from the
			// Show table
			{
				Show show = new Show( 3, new ShowDescription( 30 ) );
				session.persist( show );
			}

			// Show#4 will end up as an inverse dangling foreign-key from
			// Description because the matching row is deleted from the
			// join-table
			{
				Show show = new Show( 4, new ShowDescription( 40 ) );
				session.persist( show );
			}
		} );

		scope.inTransaction( (session) -> session.doWork( (connection) -> {
			connection.createStatement().execute( "delete from descriptions where id = 10" );
			connection.createStatement().execute( "delete from show_descriptions where description_fk = 10" );
			connection.createStatement().execute( "delete from shows where id = 3" );
			connection.createStatement().execute( "delete from show_descriptions where show_fk = 4" );
		} ) );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOneToOne(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( session -> {
			final Show show1 = session.find( Show.class, 1 );
			// we should find the show, it does exist
			assertThat( show1 ).isNotNull();
			// however, IGNORE should trigger for its description
			assertThat( show1.getDescription() ).isNull();
		} );
	}

	@Test
	public void databaseSnapshotTest(SessionFactoryScope scope) throws Exception {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final RuntimeMetamodelsImplementor runtimeMetamodels = sessionFactory.getRuntimeMetamodels();

		// Check the Show side
		scope.inTransaction( (session) -> {
			final EntityMappingType showMapping = runtimeMetamodels.getEntityMappingType( Show.class );
			final Object[] databaseSnapshot = showMapping.getEntityPersister().getDatabaseSnapshot( 1, session );

			// `Show#description` is the only state-array-contributor for Show
			assertThat( databaseSnapshot ).describedAs( "`Show` database-snapshot" ).hasSize( 1 );
			// the snapshot value for `Show#description` should be null
			assertThat( databaseSnapshot[0] ).describedAs( "`Show#description` database-snapshot value" ).isNull();
		} );

		// Check the ShowDescription side
		scope.inTransaction( (session) -> {
			final EntityMappingType descriptionMapping = runtimeMetamodels.getEntityMappingType( ShowDescription.class );
			final Object[] databaseSnapshot = descriptionMapping.getEntityPersister().getDatabaseSnapshot( 2, session );

			assertThat( databaseSnapshot ).describedAs( "`ShowDescription` database snapshot" ).isNull();
		} );

	}

	@Entity(name = "Show")
	@Table(name = "shows")
	public static class Show {

		@Id
		private Integer id;

		@OneToOne( cascade = { CascadeType.PERSIST, CascadeType.REMOVE } )
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinTable(name = "show_descriptions",
				joinColumns = @JoinColumn(name = "show_fk"),
				inverseJoinColumns = @JoinColumn(name = "description_fk")
		)
		private ShowDescription description;

		protected Show() {
		}

		public Show(Integer id, ShowDescription description) {
			this.id = id;
			this.description = description;
			if ( description != null ) {
				description.setShow( this );
			}
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ShowDescription getDescription() {
			return description;
		}

		public void setDescription(ShowDescription description) {
			this.description = description;
			description.setShow( this );
		}
	}

	@Entity(name = "ShowDescription")
	@Table(name = "descriptions")
	public static class ShowDescription {

		@Id
		@Column(name = "id")
		private Integer id;

		@NotFound(action = NotFoundAction.IGNORE)
		@OneToOne(mappedBy = "description", cascade = CascadeType.ALL)
		private Show show;

		protected ShowDescription() {
		}

		public ShowDescription(Integer id) {
			this.id = id;
		}

		public ShowDescription(Integer id, Show show) {
			this.id = id;
			this.show = show;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Show getShow() {
			return show;
		}

		public void setShow(Show show) {
			this.show = show;
		}
	}
}
