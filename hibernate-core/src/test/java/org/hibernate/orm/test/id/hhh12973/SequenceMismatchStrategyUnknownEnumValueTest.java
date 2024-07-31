/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id.hhh12973;

import org.hibernate.HibernateException;
import org.hibernate.id.SequenceMismatchStrategy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12973")
@BaseUnitTest
public class SequenceMismatchStrategyUnknownEnumValueTest {

	@Test
	public void test() {
		try {
			SequenceMismatchStrategy.interpret( "acme" );

			fail("Should throw HibernateException!");
		}
		catch (Exception e) {
			Throwable rootCause = ExceptionUtil.rootCause( e );
			assertTrue( rootCause instanceof HibernateException );
			assertEquals( "Unrecognized sequence.increment_size_mismatch_strategy value : [acme].  Supported values include [log], [exception], and [fix].", rootCause.getMessage() );
		}
	}
}
