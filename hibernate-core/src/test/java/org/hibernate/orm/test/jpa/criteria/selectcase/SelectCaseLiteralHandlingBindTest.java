/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.selectcase;

import java.util.List;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.criteria.ValueHandlingMode;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests query rendering and execution
 * when {@link CriteriaBuilder.Case} is present in the criteria
 * and the {@code hibernate.criteria.literal_handling_mode} is set to {@literal bind}.
 *
 * In both cases we expect that between predicate parameter will be bound,
 * but right hand literals of case expression will not.
 *
 * Having such query:
 * "case when generatedAlias0.commits between :param0 and :param1 then 1 when generatedAlias0.commits between :param2 and :param3 then 2 else 3 end".
 * And not:
 * "case when generatedAlias0.commits between :param0 and :param1 then :param3 when generatedAlias0.commits between :param4 and :param5 then :param6 else :param7 end".
 *
 * @author Fabio Massimo Ercoli
 */
public class SelectCaseLiteralHandlingBindTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Programmer.class };
	}

	@Test
	public void selectCaseExpression() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<Programmer> programmer = query.from( Programmer.class );

			Predicate junior = cb.between( programmer.get( "commits" ), 0, 10 );
			Predicate senior = cb.between( programmer.get( "commits" ), 11, 20 );

			CriteriaBuilder.Case<Integer> selectCase = cb.selectCase();
			selectCase.when( junior, 1 )
					.when( senior, 2 )
					.otherwise( 3 );

			query.multiselect( programmer.get( "team" ), selectCase );

			List<Tuple> resultList = entityManager.createQuery( query ).getResultList();
			assertNotNull( resultList );
			assertTrue( resultList.isEmpty() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13001")
	public void selectSumOnCaseExpression() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<Programmer> programmer = query.from( Programmer.class );

			Predicate junior = cb.between( programmer.get( "commits" ), 0, 10 );
			Predicate senior = cb.between( programmer.get( "commits" ), 11, 20 );

			CriteriaBuilder.Case<Double> selectCase = cb.selectCase();
			selectCase.when( junior, 1.1 )
					.when( senior, 2.2 )
					.otherwise( 3.3 );

			query.multiselect( programmer.get( "team" ), cb.sum( selectCase ) )
					.groupBy( programmer.get( "team" ) )
					.orderBy( cb.asc( programmer.get( "team" ) ) );

			List<Tuple> resultList = entityManager.createQuery( query ).getResultList();
			assertNotNull( resultList );
			assertTrue( resultList.isEmpty() );
		} );
	}

	@Test
	public void whereCaseExpression() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Programmer> query = cb.createQuery(Programmer.class);
			Root<Programmer> programmer = query.from( Programmer.class );

			Predicate junior = cb.between( programmer.get( "commits" ), 0, 10 );
			Predicate senior = cb.between( programmer.get( "commits" ), 11, 20 );

			CriteriaBuilder.Case<Integer> selectCase = cb.selectCase();
			selectCase.when( junior, 1 )
					.when( senior, 2 )
					.otherwise( 3 );

			query.select( programmer ).where( cb.equal( selectCase, 5 ) );

			List<Programmer> resultList = entityManager.createQuery( query ).getResultList();
			assertNotNull( resultList );
			assertTrue( resultList.isEmpty() );
		} );
	}

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();
		config.put( AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, ValueHandlingMode.BIND );
		return config;
	}

	@Entity(name = "Programmer")
	public static class Programmer {

		@Id
		private Long id;
		private String nick;

		private String team;
		private Integer commits;
	}
}
