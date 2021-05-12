/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm;

import java.util.List;

import org.hibernate.Session;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class StrandedDataHelper {
	public static void handleStrandedData(List<?> data, Session session) {
		if ( data.isEmpty() ) {
			return;
		}

		System.err.println( "Found stranded test data..." );

		data.forEach(
				(item) -> System.err.printf(
						"    - [%s # %s] : %s\n",
						item.getClass().getName(),
						session.getIdentifier( item ),
						item
				)
		);

		fail( "Test did not clean up data" );
	}
}
