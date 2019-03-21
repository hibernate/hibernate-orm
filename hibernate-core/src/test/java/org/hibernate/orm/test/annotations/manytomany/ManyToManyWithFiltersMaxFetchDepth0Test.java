/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.manytomany;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.FailureExpected;

/**
 * @author Andrea Boriero
 */
@FailureExpected("Filters not yet implemented")
public class ManyToManyWithFiltersMaxFetchDepth0Test extends ManyToManyWithFiltersTest {
	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		builer.applySetting( Environment.MAX_FETCH_DEPTH, "0" );
	}
}
