/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.manytomany;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

/**
 * Many to many tests using max_fetch_depth == 0
 *
 * @author Gail Badner
 */
@SuppressWarnings("unchecked")
public class ManyToManyMaxFetchDepth0Test extends ManyToManyTest {
	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( Environment.MAX_FETCH_DEPTH, "0" );
		super.configure( cfg );
	}
}
