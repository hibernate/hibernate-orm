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

package org.hibernate.shards.criteria;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.AggregateProjection;
import org.hibernate.criterion.Distinct;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.RowCountProjection;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.shards.strategy.exit.AvgResultsExitOperation;
import org.hibernate.shards.strategy.exit.DistinctExitOperation;
import org.hibernate.shards.strategy.exit.ExitOperationsCollector;
import org.hibernate.shards.strategy.exit.FirstResultExitOperation;
import org.hibernate.shards.strategy.exit.MaxResultsExitOperation;
import org.hibernate.shards.strategy.exit.OrderExitOperation;
import org.hibernate.shards.strategy.exit.ProjectionExitOperationFactory;
import org.hibernate.shards.util.Lists;

import java.util.List;

/**
 * Implements the ExitOperationsCollector interface for Critierias
 *
 * @author Maulik Shah
 */
public class ExitOperationsCriteriaCollector implements ExitOperationsCollector {

  // maximum number of results requested by the client
  private Integer maxResults = null;

  // index of the first result requested by the client
  private Integer firstResult = null;

  // Distinct operation applied to the Criteria
  private Distinct distinct = null;

  // Average Projection operation applied to the Criteria
  private AggregateProjection avgProjection = null;

  // Aggregate Projecton operation applied to the Criteria
  private AggregateProjection aggregateProjection = null;

  // Row Count Projection operation applied to the Criteria
  private RowCountProjection rowCountProjection;

  // The Session Factory Implementor with which the Criteria is associated
  private SessionFactoryImplementor sessionFactoryImplementor;

  // Order operations applied to the Criteria
  private List<Order> orders = Lists.newArrayList();

  // Our friendly neighborhood logger
  private final Log log = LogFactory.getLog(getClass());

  /**
   * Sets the maximum number of results requested by the client
   *
   * @param maxResults maximum number of results requested by the client
   * @return this
   */
  public ExitOperationsCollector setMaxResults(int maxResults) {
    this.maxResults = maxResults;
    return this;
  }

  /**
   * Sets the index of the first result requested by the client
   *
   * @param firstResult index of the first result requested by the client
   * @return this
   */
  public ExitOperationsCollector setFirstResult(int firstResult) {
    this.firstResult = firstResult;
    return this;
  }

  /**
   * Adds the given projection.
   *
   * @param projection the projection to add
   * @return this
   */
  public ExitOperationsCollector addProjection(Projection projection) {
    if (projection instanceof Distinct) {
      this.distinct = (Distinct)projection;
      // TODO(maulik) Distinct doesn't work yet
      log.error("Distinct is not ready yet");
      throw new UnsupportedOperationException();
    } else if(projection instanceof RowCountProjection) {
      this.rowCountProjection = (RowCountProjection) projection;
    } else if(projection instanceof AggregateProjection) {
      if (projection.toString().toLowerCase().startsWith("avg")) {
        this.avgProjection = (AggregateProjection) projection;
      } else {
        this.aggregateProjection = (AggregateProjection) projection;
      }
    } else {
      log.error("Adding an unsupported Projection: " + projection.getClass().getName());
      throw new UnsupportedOperationException();
    }
    return this;
  }

  /**
   * Add the given Order
   *
   * @param order the order to add
   * @return this
   */
  public ExitOperationsCollector addOrder(Order order) {
    this.orders.add(order);
    return this;
  }

  public List<Object> apply(List<Object> result) {
    /**
     * Herein lies the glory
     *
     * hibernate has done as much as it can, we're going to have to deal with
     * the rest in memory.
     *
     * The heirarchy of operations is this so far:
     * Distinct
     * Order
     * FirstResult
     * MaxResult
     * RowCount
     * Average
     * Min/Max/Sum
     */

    // ordering of the following operations *really* matters!
    if (distinct != null) {
      result = new DistinctExitOperation(distinct).apply(result);
    }

    for(Order order : orders) {
      result = new OrderExitOperation(order).apply(result);
    }
    if (firstResult != null) {
      result = new FirstResultExitOperation(firstResult).apply(result);
    }
    if (maxResults != null) {
      result = new MaxResultsExitOperation(maxResults).apply(result);
    }

    ProjectionExitOperationFactory factory =
        ProjectionExitOperationFactory.getFactory();

    if (rowCountProjection != null) {
      result = factory.getProjectionExitOperation(rowCountProjection, sessionFactoryImplementor).apply(result);
    }

    if (avgProjection != null) {
      result = new AvgResultsExitOperation().apply(result);
    }

    // min, max, sum
    if (aggregateProjection != null) {
      result = factory.getProjectionExitOperation(aggregateProjection, sessionFactoryImplementor).apply(result);
    }
    return result;
  }

  /**
   * Sets the session factory implementor
   * @param sessionFactoryImplementor the session factory implementor to set
   */
  public void setSessionFactory(SessionFactoryImplementor sessionFactoryImplementor) {
    this.sessionFactoryImplementor = sessionFactoryImplementor;
  }

}
