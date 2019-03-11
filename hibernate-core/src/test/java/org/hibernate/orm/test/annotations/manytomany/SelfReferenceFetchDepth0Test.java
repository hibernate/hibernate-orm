/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.manytomany;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;

/**
 * @author Andrea Boriero
 */
public class SelfReferenceFetchDepth0Test extends SelfReferenceTest {
	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		builer.applySetting( Environment.MAX_FETCH_DEPTH, "0" );
	}
}
