/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure.results;

import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.query.results.internal.jpa.JpaMappingHelper;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {Region.class, Initiative.class})
@SessionFactory
public abstract class AbstractMultipleResultMappingTests {

	@BeforeEach
	void prepareDatabase(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Region( 1, "NA", "North America") );
			session.persist( new Region( 2, "EMEA", "Europe, the Middle East, and Africa" ) );
			session.persist( new Region( 3, "APAC", "Asia Pacific" ) );

			session.persist( new Initiative( 1, "10,000 Deployments", "Q1-27" ) );
			session.persist( new Initiative( 2, "2,000,000,000 Revenue", "Q4-27" ) );
		} );
	}

	@AfterEach
	void cleanupDatabase(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	protected void prepareProcedure(StoredProcedureQuery call) {
	}

	@Test
	void testUpFrontDeclaration(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final StoredProcedureQuery procedureCall = session.createStoredProcedureQuery(
					"get_results",
					Region.class,
					Initiative.class
			);
			prepareProcedure( procedureCall );

			final boolean hasResults = procedureCall.execute();
			assertThat( hasResults ).isTrue();

			final List<Region> regions = procedureCall.getResultList( Region.class );
			assertThat( regions ).hasSize( 3 );

			assertThat( procedureCall.hasMoreResults() ).isTrue();

			final List<Initiative> initiatives = procedureCall.getResultList( Initiative.class );
			assertThat( initiatives ).hasSize( 2 );
		} );
	}

	@Test
	void testDelayedClassDeclaration(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final StoredProcedureQuery procedureCall = session.createStoredProcedureQuery(
					"get_results"
			);
			prepareProcedure( procedureCall );

			final boolean hasResults = procedureCall.execute();
			assertThat( hasResults ).isTrue();

			final List<Region> regions = procedureCall.getResultList( Region.class );
			assertThat( regions ).hasSize( 3 );

			assertThat( procedureCall.hasMoreResults() ).isTrue();

			final List<Initiative> initiatives = procedureCall.getResultList( Initiative.class );
			assertThat( initiatives ).hasSize( 2 );
		} );
	}

	@Test
	void testDelayedMappingDeclaration(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final StoredProcedureQuery procedureCall = session.createStoredProcedureQuery(
					"get_results"
			);
			prepareProcedure( procedureCall );

			final boolean hasResults = procedureCall.execute();
			assertThat( hasResults ).isTrue();

			final List<Region> regions = procedureCall.getResultList( ResultSetMapping.entity(
					Region.class,
					ResultSetMapping.field( Region_.id, "id" ),
					ResultSetMapping.field( Region_.code, "code" ),
					ResultSetMapping.field( Region_.name, "name" )
			) );
			assertThat( regions ).hasSize( 3 );

			assertThat( procedureCall.hasMoreResults() ).isTrue();

			final List<Initiative> initiatives = procedureCall.getResultList( ResultSetMapping.entity(
					Initiative.class,
					ResultSetMapping.field( Initiative_.id, "id" ),
					ResultSetMapping.field( Initiative_.name, "name" ),
					ResultSetMapping.field( Initiative_.targetQuarter, "target_quarter" )
			) );
			assertThat( initiatives ).hasSize( 2 );
		} );
	}

	@Test
	@FailureExpected
	void testUpFrontDeclarationWithOverridesBaseline(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final StoredProcedureQuery procedureCall = session.createStoredProcedureQuery(
					"get_results",
					Initiative.class,
					Region.class
			);
			prepareProcedure( procedureCall );

			final boolean hasResults = procedureCall.execute();
			assertThat( hasResults ).isTrue();

			final List<Region> regions = procedureCall.getResultList();
			assertThat( regions ).hasSize( 3 );

			assertThat( procedureCall.hasMoreResults() ).isTrue();

			final List<Initiative> initiatives = procedureCall.getResultList();
			assertThat( initiatives ).hasSize( 2 );
		} );
	}

	@Test
	@FailureExpected
	void testUpFrontDeclarationWithOverridesBaseline2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final StoredProcedureQuery procedureCall = session.createStoredProcedureQuery(
					"get_results",
					Initiative.class,
					Region.class
			);
			prepareProcedure( procedureCall );

			final boolean hasResults = procedureCall.execute();
			assertThat( hasResults ).isTrue();

			final List<Region> regions = procedureCall.getResultList( Region.class );
			assertThat( regions ).hasSize( 3 );

			assertThat( procedureCall.hasMoreResults() ).isTrue();

			final List<Initiative> initiatives = procedureCall.getResultList( Initiative.class );
			assertThat( initiatives ).hasSize( 2 );
		} );
	}

	@Test
	void testUpFrontDeclarationWithOverrides(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final StoredProcedureQuery procedureCall = session.createStoredProcedureQuery(
					"get_results",
					Object.class,
					Initiative.class
			);
			prepareProcedure( procedureCall );

			final boolean hasResults = procedureCall.execute();
			assertThat( hasResults ).isTrue();

			final List<Region> regions = procedureCall.getResultList( JpaMappingHelper.makeJpaMapping(Region.class, factoryScope.getSessionFactory()) );
			assertThat( regions ).hasSize( 3 );

			assertThat( procedureCall.hasMoreResults() ).isTrue();

			final List<Initiative> initiatives = procedureCall.getResultList( Initiative.class );
			assertThat( initiatives ).hasSize( 2 );
		} );
	}

	@Test
	void testUpFrontDeclarationWithOverrides2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final StoredProcedureQuery procedureCall = session.createStoredProcedureQuery(
					"get_results",
					Object.class
			);
			prepareProcedure( procedureCall );

			final boolean hasResults = procedureCall.execute();
			assertThat( hasResults ).isTrue();

			final List<Region> regions = procedureCall.getResultList( JpaMappingHelper.makeJpaMapping(Region.class, factoryScope.getSessionFactory()) );
			assertThat( regions ).hasSize( 3 );
			assertThat( regions.get(0) ).isInstanceOf( Region.class );

			assertThat( procedureCall.hasMoreResults() ).isTrue();

			final List<Initiative> initiatives = procedureCall.getResultList( Initiative.class );
			assertThat( initiatives ).hasSize( 2 );
			assertThat( initiatives.get(0) ).isInstanceOf( Initiative.class );
		} );
	}

	@Test
	void testTooManyMappings(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final StoredProcedureQuery procedureCall = session.createStoredProcedureQuery(
					"get_results",
					Region.class,
					Initiative.class,
					Region.class
			);
			prepareProcedure( procedureCall );

			final boolean hasResults = procedureCall.execute();
			assertThat( hasResults ).isTrue();

			final List<?> regions = procedureCall.getResultList();
			assertThat( regions ).hasSize( 3 );
			assertThat( regions.get(0) ).isInstanceOf( Region.class );

			assertThat( procedureCall.hasMoreResults() ).isTrue();

			final List<?> initiatives = procedureCall.getResultList();
			assertThat( initiatives ).hasSize( 2 );
			assertThat( initiatives.get(0) ).isInstanceOf( Initiative.class );

			// we simply ignore the 3rd mapping.
			// todo (jpa4) : should this be an error?
		} );
	}

	@Test
	void testTooFewMappings(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final StoredProcedureQuery procedureCall = session.createStoredProcedureQuery(
					"get_results",
					Region.class
			);
			prepareProcedure( procedureCall );

			final boolean hasResults = procedureCall.execute();
			assertThat( hasResults ).isTrue();

			final List<?> regions = procedureCall.getResultList();
			assertThat( regions ).hasSize( 3 );

			assertThat( procedureCall.hasMoreResults() ).isTrue();

			final List<?> initiatives = procedureCall.getResultList();
			assertThat( initiatives ).hasSize( 2 );
			assertThat( initiatives.get( 0 ).getClass().isArray() ).isTrue();
			assertThat( (Object[]) initiatives.get( 0 ) ).hasSize( 3 );
		} );
	}

	@Test
	void testOverrides(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final StoredProcedureQuery procedureCall = session.createStoredProcedureQuery(
					"get_results",
					Region.class
			);
			prepareProcedure( procedureCall );

			final boolean hasResults = procedureCall.execute();
			assertThat( hasResults ).isTrue();

			final List<?> regions = procedureCall.getResultList();
			assertThat( regions ).hasSize( 3 );
			assertThat( regions.get(0) ).isInstanceOf( Region.class );

			assertThat( procedureCall.hasMoreResults() ).isTrue();

			final List<?> initiatives = procedureCall.getResultList();
			assertThat( initiatives ).hasSize( 2 );
			assertThat( initiatives.get( 0 ).getClass().isArray() ).isTrue();
			assertThat( (Object[]) initiatives.get( 0 ) ).hasSize( 3 );
		} );
	}
}
