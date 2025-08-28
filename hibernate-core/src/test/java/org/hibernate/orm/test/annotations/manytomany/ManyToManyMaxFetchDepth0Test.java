/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

/**
 * Many to many tests using max_fetch_depth == 0
 *
 * @author Gail Badner
 */
public class ManyToManyMaxFetchDepth0Test extends ManyToManyTest {
	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( Environment.MAX_FETCH_DEPTH, 0 );
		super.configure( cfg );
	}
}
