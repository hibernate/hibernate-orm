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
import org.hibernate.criterion.Projection;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Pair;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Maulik Shah
 */
public class ShardedAvgExitOperation implements ProjectionExitOperation {

  private final Log log = LogFactory.getLog(getClass());

  public ShardedAvgExitOperation(Projection projection) {
    log.error("not ready to use!");
    throw new UnsupportedOperationException();
  }

  public List<Object> apply(List<Object> results) {
    BigDecimal value = new BigDecimal(0.0);
    BigDecimal count = new BigDecimal(0.0);
    @SuppressWarnings("unchecked")
    List<Pair<Double, Integer>> pairList = (List<Pair<Double, Integer>>) (List) results;
    for(Pair<Double, Integer> pair : pairList) {
      // we know the order of the pair (avg, count) by convention of ShardedAvgProjection
      value = value.add(new BigDecimal(pair.first));
      count = count.add(new BigDecimal(pair.second));
    }
    return Lists.newArrayList((Object)value.divide(count));
  }
}
