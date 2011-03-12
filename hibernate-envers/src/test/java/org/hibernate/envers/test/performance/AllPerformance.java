package org.hibernate.envers.test.performance;

import java.io.IOException;

public class AllPerformance {
	public static void main(String[] args) throws IOException {
		new InsertsPerformance().test(10);
		new ComplexInsertPerformance().test(10);
		new UpdatesPerformance().test(10);
		new InsertsOneTransactionPerformance().test(10);
	}
}
