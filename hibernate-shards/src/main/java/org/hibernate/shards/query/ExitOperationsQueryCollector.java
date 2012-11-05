/**
 * Copyright (C) 2006 Google Inc.
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

package org.hibernate.shards.query;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.shards.strategy.exit.ExitOperationsCollector;
import org.hibernate.shards.strategy.exit.FirstResultExitOperation;
import org.hibernate.shards.strategy.exit.MaxResultsExitOperation;

import java.util.List;

/**
 * Exit operations for queries is essentially not implemented. Its intended use
 * is to record a set of aggregation type operations to be executed on the
 * combined results for a query executed on each shard.
 *
 * We do implement setMaxResults and setFirstResult as these operations do not
 * require parsing the query string.
 *
 * {@inheritDoc}
 *
 * @author Maulik Shah
 */
public class ExitOperationsQueryCollector implements ExitOperationsCollector {

  // maximum number of results requested by the client
  private Integer maxResults = null;

  // index of the first result requested by the client
  private Integer firstResult = null;

  public List<Object> apply(List<Object> result) {
    if (firstResult != null) {
      result = new FirstResultExitOperation(firstResult).apply(result);
    }
    if (maxResults != null) {
      result = new MaxResultsExitOperation(maxResults).apply(result);
    }

    return result;
  }

  public void setSessionFactory(SessionFactoryImplementor sessionFactoryImplementor) {
    throw new UnsupportedOperationException();
  }

  public ExitOperationsCollector setMaxResults(int maxResults) {
    this.maxResults = maxResults;
    return this;
  }

  public ExitOperationsCollector setFirstResult(int firstResult) {
    this.firstResult = firstResult;
    return this;
  }

}
