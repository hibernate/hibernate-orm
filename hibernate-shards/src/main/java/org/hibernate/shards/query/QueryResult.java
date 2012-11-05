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

package org.hibernate.shards.query;

import org.hibernate.shards.Shard;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Maulik Shah
 */
public class QueryResult {

  private final Map<Shard, List> resultMap = Maps.newHashMap();

  private final List<Object> entityList = Lists.newArrayList();

  public Map<Shard, List> getResultMap() {
    return Collections.unmodifiableMap(resultMap);
  }

  public void add(Shard shard, List<Object> list) {
    resultMap.put(shard, list);
    entityList.addAll(list);
  }

  public void add(QueryResult result) {
    resultMap.putAll(result.getResultMap());
    entityList.addAll(result.getEntityList());
  }

  public List<Object> getEntityList() {
    return entityList;
  }

}
