/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.hhh12973;

import org.hibernate.HibernateException;
import org.hibernate.id.SequenceMismatchStrategy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.jupiter.api.Test;

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
		}
	}
}
