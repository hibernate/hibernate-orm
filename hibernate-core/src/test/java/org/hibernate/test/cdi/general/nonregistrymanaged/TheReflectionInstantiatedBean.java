/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cdi.general.nonregistrymanaged;

import javax.enterprise.inject.Vetoed;

/**
 * @author Yoann Rodiere
 */
@Vetoed
public class TheReflectionInstantiatedBean {

	public void ensureInitialized() {
		// No-op
	}

}
