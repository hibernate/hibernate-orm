/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.naming;

import org.hibernate.boot.model.naming.ImplicitIndexColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNameSource;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dmytro Bondar
 */
@BaseUnitTest
public class ImplicitIndexColumnNameSourceTest {

	@Test
	@JiraKey(value = "HHH-10810")
	public void testExtensionImplicitNameSource() {
		assertThat( ImplicitNameSource.class ).isAssignableFrom( ImplicitIndexColumnNameSource.class );
	}

}
