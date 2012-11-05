/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.strategy.exit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.AggregateProjection;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * @author Maulik Shah
 */
public class AggregateExitOperation implements ProjectionExitOperation {

  private final SupportedAggregations aggregate;

  private final String fieldName;

  private final Log log = LogFactory.getLog(getClass());

  private enum SupportedAggregations {

    SUM("sum"),
    MIN("min"),
    MAX("max");

    private final String aggregate;

    private SupportedAggregations(String s) {
      this.aggregate = s;
    }

    public String getAggregate() {
      return aggregate;
    }

  }

  public AggregateExitOperation(AggregateProjection projection) {
    /**
     * an aggregateProjection's toString returns
     * min( ..., max( ..., sum( ..., or avg( ...
     * we just care about the name of the function
     * which happens to be before the first left parenthesis
     */
    String projectionAsString = projection.toString();
    String aggregateName = projectionAsString.substring(0,projectionAsString.indexOf("("));
    this.fieldName = projectionAsString.substring(projectionAsString.indexOf("(")+1, projectionAsString.indexOf(")"));
    try {
      this.aggregate = SupportedAggregations.valueOf(aggregateName.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Use of unsupported aggregate: "+ aggregateName);
      throw e;
    }
  }

  public List<Object> apply(List<Object> results) {

    List<Object> nonNullResults = ExitOperationUtils.getNonNullList(results);

    switch(aggregate) {
      case MAX:
        return Collections.singletonList((Object) Collections.max(ExitOperationUtils.getComparableList(nonNullResults)));
      case MIN:
        return Collections.singletonList((Object) Collections.min(ExitOperationUtils.getComparableList(nonNullResults)));
      case SUM:
        return Collections.<Object>singletonList(getSum(nonNullResults, fieldName));
      default:
        log.error("Aggregation Projection is unsupported: "+aggregate);
        throw new UnsupportedOperationException("Aggregation Projection is unsupported: "+ aggregate);
    }
  }

  private BigDecimal getSum(List<Object> results, String fieldName) {
    BigDecimal sum = new BigDecimal(0.0);
    for (Object obj : results) {
      Number num = getNumber(obj, fieldName);
      sum = sum.add(new BigDecimal(num.toString()));
    }
    return sum;
  }

  private Number getNumber(Object obj, String fieldName) {
    return (Number) ExitOperationUtils.getPropertyValue(obj, fieldName);
  }

}
