/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.ql;

import org.hibernate.query.sqm.StrictJpaComplianceViolation;

import org.hibernate.testing.junit5.FailureExpected;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Testing strict JPQL compliance checking
 *
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class StrictJpqlComplianceTests extends LenientJpqlComplianceTests {
	@Override
	protected boolean strictJpaCompliance() {
		return true;
	}

	@Override
	protected void validateSuccess() {
		fail( "expected violation" );
	}

	@Override
	protected void validateViolation(StrictJpaComplianceViolation violation) {
		assertThat( violation.getType(), notNullValue() );
		assertThat( violation.getType(), is( getCurrentViolationBeingChecked() ) );
	}

	@Test
	@Override
	public void testAliasedFetchJoin() {
		super.testAliasedFetchJoin();
	}

	@Test
	@Override
	@FailureExpected( "Need to add strict-compliance checking for indexed element access" )
	public void testIndexedElementReference() {
		super.testIndexedElementReference();
	}
}
