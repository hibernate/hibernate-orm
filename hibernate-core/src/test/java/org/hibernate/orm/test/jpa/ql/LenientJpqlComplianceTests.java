/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.ql;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.produce.domain.Person;
import org.hibernate.orm.test.support.domains.gambit.EntityOfLists;
import org.hibernate.orm.test.support.domains.gambit.EntityOfMaps;
import org.hibernate.orm.test.support.domains.gambit.EntityOfSets;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.ALIASED_FETCH_JOIN;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.FUNCTION_CALL;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.IMPLICIT_SELECT;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.INDEXED_ELEMENT_REFERENCE;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.LIMIT_OFFSET_CLAUSE;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.SUBQUERY_ORDER_BY;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.UNMAPPED_POLYMORPHISM;
import static org.hibernate.query.sqm.StrictJpaComplianceViolation.Type.VALUE_FUNCTION_ON_NON_MAP;

/**
 * The lenient form of jpql compliance checking.  subclassed by the strict check form
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class LenientJpqlComplianceTests extends BaseSqmUnitTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Person.class );
		metadataSources.addAnnotatedClass( EntityOfLists.class );
		metadataSources.addAnnotatedClass( EntityOfSets.class );
		metadataSources.addAnnotatedClass( EntityOfMaps.class );
	}

	private StrictJpaComplianceViolation.Type violationChecked;

	protected StrictJpaComplianceViolation.Type getCurrentViolationBeingChecked() {
		if ( violationChecked == null ) {
			throw new IllegalStateException( "Current StrictJpaComplianceViolation.Type being checked is not set" );
		}

		return violationChecked;
	}

	protected void validateSuccess() {
	}

	protected void validateViolation(StrictJpaComplianceViolation violation) {
		throw violation;
	}

	@AfterEach
	public void resetViolationChecked() {
		violationChecked = null;
	}

	@Test
	public void testImplicitSelectClause() {
		test( "from Person", IMPLICIT_SELECT );
	}

	private void test(String hql, StrictJpaComplianceViolation.Type violationChecked) {
		this.violationChecked = violationChecked;

		try {
			interpretSelect( hql );
			validateSuccess();
		}
		catch (StrictJpaComplianceViolation v) {
			validateViolation( v );
		}
		finally {
			this.violationChecked = null;
		}
	}

	@Test
	public void testUnmappedPolymorphicReference() {
		test( "select o from java.lang.Object o", UNMAPPED_POLYMORPHISM );
	}

	@Test
	public void testAliasedFetchJoin() {
		test( "select o from Person o join fetch o.mate e", ALIASED_FETCH_JOIN );
	}

	@Test
	public void testNonStandardFunctionCall() {
		test( "select o from Person o where my_func(o.nickName) = 1", FUNCTION_CALL );
	}

	@Test
	public void testLimitOffset() {
		test( "select o from Person o limit 1 offset 1", LIMIT_OFFSET_CLAUSE );
	}

	@Test
	public void testSubqueryOrderBy() {
		test(
				"select o from Person o where o.mate = ( select oSub from Person oSub order by oSub.nickName limit 1 )",
				SUBQUERY_ORDER_BY
		);
	}

	@Test
	public void testCollectionValueFunctionOnNonMap() {
		test(
				"select value(b) from EntityOfLists e join e.listOfBasics b",
				VALUE_FUNCTION_ON_NON_MAP
		);
		test(
				"select value(b) from EntityOfSets e join e.setOfBasics b",
				VALUE_FUNCTION_ON_NON_MAP
		);
		test(
				"select elements(b) from EntityOfLists e join e.listOfBasics b",
				VALUE_FUNCTION_ON_NON_MAP
		);
		test(
				"select elements(b) from EntityOfSets e join e.setOfBasics b",
				VALUE_FUNCTION_ON_NON_MAP
		);
	}

	@Test
	public void testIndexedElementReference() {
		test(
				"select b[0] from EntityOfLists e join e.listOfBasics b",
				INDEXED_ELEMENT_REFERENCE
		);
		test(
				"select b['name'] from EntityOfMaps e join e.basicToBasicMap b",
				INDEXED_ELEMENT_REFERENCE
		);
	}
}
