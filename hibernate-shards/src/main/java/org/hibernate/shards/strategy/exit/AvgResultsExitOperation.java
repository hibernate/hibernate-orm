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
import org.hibernate.shards.util.Pair;

import java.util.Collections;
import java.util.List;

/**
 * Performs post-processing on a result set that has had an average projection
 * applied.
 *
 * This may not yield the exact same result as you'd get if you ran the query
 * on a single shard because there seems to be some platform-specific wiggle.
 * Here's a specific example:
 * On hsqldb, if you have a column of type DECIMAL(10, 4) and you ask for the
 * average of the values in that column, you get the floor of the result.
 * On MySQL, if you have a column of the same type, you get a result back with
 * the expected precision.  So, um, just be careful.
 *
 * @author maxr@google.com (Max Ross)
 */
public class AvgResultsExitOperation implements ExitOperation {

  private final Log log = LogFactory.getLog(getClass());

  public List<Object> apply(List<Object> results) {
    List<Object> nonNullResults = ExitOperationUtils.getNonNullList(results);
    Double total = null;
    int numResults = 0;
    for(Object result : nonNullResults) {
      /**
       * We expect all entries to be Object arrays.
       * the first entry in the array is the average (a double)
       * the second entry in the array is the number of rows that were examined
       * to arrive at the average.
       */
      Pair<Double, Integer> pair = getResultPair(result);
      Double shardAvg = pair.first;
      if(shardAvg == null) {
        // if there's no result from this shard it doesn't go into the
        // calculation.  This is consistent with how avg is implemented
        // in the database
        continue;
      }
      int shardResults = pair.second;
      Double shardTotal = shardAvg * shardResults;
      if(total == null) {
        total = shardTotal;
      } else {
        total += shardTotal;
      }
      numResults += shardResults;
    }
    if(numResults == 0 || total == null) {
      return Collections.singletonList(null);
    }
    return Collections.<Object>singletonList(total / numResults);
  }

  private Pair<Double, Integer> getResultPair(Object result) {
    if(!(result instanceof Object[])) {
      final String msg =
          "Wrong type in result list.  Expected " + Object[].class +
              " but found " + result.getClass();
      log.error(msg);
      throw new IllegalStateException(msg);
    }
    Object[] resultArr = (Object[]) result;
    if(resultArr.length != 2) {
      final String msg =
          "Result array is wrong size.  Expected 2 " +
              " but found " + resultArr.length;
      log.error(msg);
      throw new IllegalStateException(msg);
    }
    return Pair.of((Double) resultArr[0], (Integer) resultArr[1]);
  }
}
