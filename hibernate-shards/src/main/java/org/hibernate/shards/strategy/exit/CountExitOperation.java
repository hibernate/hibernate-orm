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

import org.hibernate.criterion.CountProjection;
import org.hibernate.criterion.Projection;
import org.hibernate.shards.internal.ShardsMessageLogger;
import org.hibernate.shards.util.Preconditions;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * @author Maulik Shah
 */
public class CountExitOperation implements ProjectionExitOperation {

  public static final ShardsMessageLogger LOG = Logger.getMessageLogger(ShardsMessageLogger.class, CountExitOperation.class.getName());

  private final boolean distinct;

  public CountExitOperation(final Projection projection) {
    Preconditions.checkState(projection instanceof CountProjection);

    distinct = projection.toString().indexOf("distinct") != -1;

    /**
     * TODO(maulik) we need to figure out how to work with distinct
     * the CountProjection will return a count that is distinct for a particular
     * shard, however, without knowing which elements it has seen, we cannot
     * aggregate the counts.
     */

    LOG.exitOperationNotReadyToUse();
    throw new UnsupportedOperationException();
  }

  public List<Object> apply(List<Object> results) {
    // TODO(maulik) implement this
    LOG.exitOperationNotReadyToUse();
    throw new UnsupportedOperationException();
  }
}
