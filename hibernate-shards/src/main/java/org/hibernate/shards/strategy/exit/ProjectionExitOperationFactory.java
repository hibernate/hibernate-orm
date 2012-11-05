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

import org.hibernate.criterion.AggregateProjection;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.RowCountProjection;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * @author Maulik Shah
 */
public class ProjectionExitOperationFactory {

  private final static ProjectionExitOperationFactory projectionExitOperationFactory
      = new ProjectionExitOperationFactory();

  private ProjectionExitOperationFactory() {}

  public static ProjectionExitOperationFactory getFactory() {
    return projectionExitOperationFactory;
  }

  public ProjectionExitOperation getProjectionExitOperation(Projection projection, SessionFactoryImplementor sessionFactoryImplementor) {
    if (projection instanceof RowCountProjection) {
      return new RowCountExitOperation(projection);
    }
    if (projection instanceof AggregateProjection) {
      return new AggregateExitOperation((AggregateProjection) projection);
    }


    // TODO(maulik) support these projections, hopefully not by creating ShardedProjections
    // Projection rowCount() {
    // AggregateProjection avg(String propertyName) {
    // CountProjection count(String propertyName) {
    // Projection distinct(Projection proj) {
    // ProjectionList projectionList() {
    // CountProjection countDistinct(String propertyName) {
    // Projection sqlProjection(String sql, String[] columnAliases, Type[] types) {
    // Projection sqlGroupProjection(String sql, String groupBy, String[] columnAliases, Type[] types) {
    // PropertyProjection groupProperty(String propertyName) {
    // PropertyProjection property(String propertyName) {
    // IdentifierProjection id() {
    // Projection alias(Projection projection, String alias) {

    throw new UnsupportedOperationException(
        "This projection is unsupported: " + projection.getClass());
  }

}
