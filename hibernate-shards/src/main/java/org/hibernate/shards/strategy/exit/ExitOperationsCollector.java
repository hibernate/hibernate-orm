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

import org.hibernate.engine.SessionFactoryImplementor;

import java.util.List;

/**
 * Classes that implement this interface are designed to manage the results
 * of a incomplete execution of a query/critieria. For example, with averages
 * the result of each query/critieria should be a list objects on which to
 * calculate the average, rather than the avgerages on each shard. Or the
 * the sum of maxResults(200) should be the sum of only 200 results, not the
 * sum of the sums of 200 results per shard.
 *
 * @author Maulik Shah
 */
public interface ExitOperationsCollector {

  List<Object> apply(List<Object> result);

  void setSessionFactory(SessionFactoryImplementor sessionFactoryImplementor);

}
