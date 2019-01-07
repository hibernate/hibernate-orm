/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.ql;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.internal.JpaComplianceImpl;
import org.hibernate.orm.test.query.sqm.produce.domain.Person;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.tree.SqmSelectStatement;

import org.hibernate.testing.orm.domain.gambit.EntityOfLists;
import org.hibernate.testing.orm.domain.gambit.EntityOfMaps;
import org.hibernate.testing.orm.domain.gambit.EntityOfSets;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.ALIASED_FETCH_JOIN;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.FUNCTION_CALL;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.IMPLICIT_SELECT;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.INDEXED_ELEMENT_REFERENCE;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.LIMIT_OFFSET_CLAUSE;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.SUBQUERY_ORDER_BY;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.UNMAPPED_POLYMORPHISM;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.VALUE_FUNCTION_ON_NON_MAP;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for various JPA query language compliance for cases where Hibernate
 * supports "extended behavior".
 *
 * NOTE : this is basically a PoC of using ParameterizedTest, combining strict and lenient
 * compliance testing into a single test class using ParameterizedTest + an argument source
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class JpqlComplianceTests extends BaseSessionFactoryFunctionalTest {

	@ComplianceTesting
	public void testImplicitSelectClause(boolean strict) {
		test( "from Person", IMPLICIT_SELECT, strict );
	}

	@ComplianceTesting
	public void testUnmappedPolymorphicReference(boolean strict) {
		test( "select o from java.lang.Object o", UNMAPPED_POLYMORPHISM, strict );
	}

	@ComplianceTesting
	public void testAliasedFetchJoin(boolean strict) {
		test( "select o from Person o join fetch o.mate e", ALIASED_FETCH_JOIN, strict );
	}

	@ComplianceTesting
	public void testNonStandardFunctionCall(boolean strict) {
		test( "select o from Person o where my_func(o.nickName) = 1", FUNCTION_CALL, strict );
	}

	@ComplianceTesting
	public void testLimitOffset(boolean strict) {
		test( "select o from Person o limit 1 offset 1", LIMIT_OFFSET_CLAUSE, strict );
	}

	@ComplianceTesting
	public void testSubqueryOrderBy(boolean strict) {
		test(
				"select o from Person o where o.mate = ( select oSub from Person oSub order by oSub.nickName limit 1 )",
				SUBQUERY_ORDER_BY,
				strict
		);
	}

	@ComplianceTesting
	public void testCollectionValueFunctionOnNonMap(boolean strict) {
		test(
				"select value(b) from EntityOfLists e join e.listOfBasics b",
				VALUE_FUNCTION_ON_NON_MAP,
				strict
		);
		test(
				"select value(b) from EntityOfSets e join e.setOfBasics b",
				VALUE_FUNCTION_ON_NON_MAP,
				strict
		);
		test(
				"select elements(b) from EntityOfLists e join e.listOfBasics b",
				VALUE_FUNCTION_ON_NON_MAP,
				strict
		);
		test(
				"select elements(b) from EntityOfSets e join e.setOfBasics b",
				VALUE_FUNCTION_ON_NON_MAP,
				strict
		);
	}

	@ComplianceTesting
	public void testIndexedElementReference(boolean strict) {
		if ( strict ) {
			return;
		}

		test(
				"select b[0] from EntityOfLists e join e.listOfBasics b",
				INDEXED_ELEMENT_REFERENCE,
				strict
		);
		test(
				"select b['name'] from EntityOfMaps e join e.basicToBasicMap b",
				INDEXED_ELEMENT_REFERENCE,
				strict
		);
	}

	@ParameterizedTest
	@ValueSource( strings = {"false","true"} )
	@Retention( RetentionPolicy.RUNTIME )
	public @interface ComplianceTesting {
	}



	private void test(String hql, StrictJpaComplianceViolation.Type violationChecked, boolean strict) {
		try {
			interpretSelect( hql, strict );

			// the HQL parsed successfully
			//		- this is considered a failure if strict == true
			if ( strict ) {
				fail( "expected violation" );
			}
		}
		catch (StrictJpaComplianceViolation violation) {
			// we had a compliance violation
			//		- this is a failure only if strict != true
			if ( !strict ) {
				throw violation;
			}
			else {
				assertThat( violation.getType(), notNullValue() );
				assertThat( violation.getType(), is( violationChecked ) );
			}
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				EntityOfLists.class,
				EntityOfSets.class,
				EntityOfMaps.class
		};
	}

	@Override
	protected boolean exportSchema() {
		return false;
	}

	protected SqmSelectStatement interpretSelect(String hql, boolean strict) {
		final SessionFactoryImplementor factory = sessionFactory();

		adjustComplianceSetting( factory, strict );

		return (SqmSelectStatement) factory.getQueryEngine().getSemanticQueryProducer().interpret( hql );
	}

	private void adjustComplianceSetting(SessionFactoryImplementor factory, boolean strict) {
		final JpaComplianceImpl jpaCompliance = (JpaComplianceImpl) factory.getSessionFactoryOptions().getJpaCompliance();
		try {
			final Field queryComplianceField = JpaComplianceImpl.class.getDeclaredField( "queryCompliance" );
			queryComplianceField.setAccessible( true );

			queryComplianceField.set( jpaCompliance, strict );
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}
}
