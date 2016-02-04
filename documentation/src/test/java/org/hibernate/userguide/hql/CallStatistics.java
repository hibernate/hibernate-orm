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
    private final double abg;

    public CallStatistics(long count, long total, int min, int max, double abg) {
        this.count = count;
        this.total = total;
        this.min = min;
        this.max = max;
        this.abg = abg;
    }

    //Getters and setters omitted for brevity
}
//end::hql-select-clause-dynamic-instantiation-example[]
