package org.hibernate.query.spi;

/**
 * @author Steve Ebersole
 */
public interface NonSelectQueryPlan extends QueryPlan {
	int executeUpdate(DomainQueryExecutionContext executionContext);
}
