/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.testing.orm.junit.DialectContext;
import org.junit.Test;

public class DialectContextTest {

	@Test
	public void smoke() {
		Dialect current = DialectContext.getDialect();
		assertThat( current ).isNotNull();
	}
}
