/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.hhh12973;

import org.hibernate.id.SequenceMismatchStrategy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12973")
@BaseUnitTest
public class SequenceMismatchStrategyNullValueTest {

	@Test
	public void test() {
		assertEquals( SequenceMismatchStrategy.EXCEPTION, SequenceMismatchStrategy.interpret( null ) );
	}
}
