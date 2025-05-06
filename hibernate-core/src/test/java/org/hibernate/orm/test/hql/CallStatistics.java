/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

/**
 * @author Vlad Mihalcea
 */
//tag::hql-select-clause-dynamic-instantiation-example[]
public class CallStatistics {

	private final long count;
	private final long total;
	private final int min;
	private final int max;
	private final double avg;

	public CallStatistics(long count, long total, int min, int max, double avg) {
		this.count = count;
		this.total = total;
		this.min = min;
		this.max = max;
		this.avg = avg;
	}

	//Getters and setters omitted for brevity
}
//end::hql-select-clause-dynamic-instantiation-example[]
