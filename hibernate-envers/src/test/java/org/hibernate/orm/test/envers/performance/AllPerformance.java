/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.performance;

import java.io.IOException;

public class AllPerformance {
	public static void main(String[] args) throws IOException {
		new InsertsPerformance().test( 10 );
		new ComplexInsertPerformance().test( 10 );
		new UpdatesPerformance().test( 10 );
		new InsertsOneTransactionPerformance().test( 10 );
	}
}
