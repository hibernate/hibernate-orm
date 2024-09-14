/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.domain.Person;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.junit.jupiter.api.Test;

/**
 * @author Nahtan Xu
 */
public class JpaComplianceDisallowFQNTests extends BaseSqmUnitTest {

	@Override
	protected boolean strictJpaCompliance() {
		return true;
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	@ExpectedException(StrictJpaComplianceViolation.class)
	public void testQuery() {
		interpretSelect( String.format( "select p from %s p", Person.class.getName() ) );
	}

	@Test
	@ExpectedException(StrictJpaComplianceViolation.class)
	public void testUpdate() {
		interpretSelect( String.format( "update %s set numberOfToes = 0", Person.class.getName() ) );
	}

	@Test
	@ExpectedException(StrictJpaComplianceViolation.class)
	public void testDelete() {
		interpretSelect( String.format( "delete %s", Person.class.getName() ) );
	}

}
