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

import org.hibernate.shards.Shard;
import org.hibernate.shards.util.Lists;

import java.util.List;

/**
 * Threadsafe ExistStrategy that concatenates all the lists that are added.
 *
 * @author maxr@google.com (Max Ross)
 */
public class ConcatenateListsExitStrategy implements ExitStrategy<List<Object>> {

  private final List<Object> result = Lists.newArrayList();

  public synchronized boolean addResult(List<Object> oneResult, Shard shard) {
    result.addAll(oneResult);
    return false;
  }

  public List<Object> compileResults(ExitOperationsCollector exitOperationsCollector) {
    return exitOperationsCollector.apply(result);
  }
}
