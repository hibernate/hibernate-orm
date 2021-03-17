/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.hql;

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
