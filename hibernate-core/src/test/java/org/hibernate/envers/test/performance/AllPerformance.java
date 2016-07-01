/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.performance;

import java.io.IOException;

public class AllPerformance {
	public static void main(String[] args) throws IOException {
		new InsertsPerformance().test( 10 );
		new ComplexInsertPerformance().test( 10 );
		new UpdatesPerformance().test( 10 );
		new InsertsOneTransactionPerformance().test( 10 );
	}
}
