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

import java.util.List;

/**
 * @author Maulik Shah
 */
public class MaxResultsExitOperation implements ExitOperation {

  private final int maxResults;

  public MaxResultsExitOperation(int maxResults) {
    this.maxResults = maxResults;
  }

  public List<Object> apply(List<Object> results) {
    List<Object> nonNullResults = ExitOperationUtils.getNonNullList(results);
    return nonNullResults.subList(0, Math.min(nonNullResults.size(), maxResults));
  }
}
