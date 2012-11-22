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

import org.hibernate.Query;
import org.hibernate.shards.session.ShardedSessionException;

import java.util.Map;

/**
 * @author Maulik Shah
 */
public class SetPropertiesEvent implements QueryEvent {

  private enum MethodSig {
    OBJECT, MAP
  }

  private final MethodSig sig;
  private final Object bean;
  private final Map map;

  public SetPropertiesEvent(Object bean) {
    this(MethodSig.OBJECT, bean, null);
  }

  public SetPropertiesEvent(Map map) {
    this(MethodSig.MAP, null, map);
  }

  private SetPropertiesEvent(MethodSig sig, Object bean, Map map) {
    this.sig = sig;
    this.bean = bean;
    this.map = map;
  }

  public void onEvent(Query query) {
    switch (sig) {
      case OBJECT:
        query.setProperties(bean);
        break;
      case MAP:
        query.setProperties(map);
        break;
      default:
        throw new ShardedSessionException(
            "Unknown sig in SetPropertiesEvent: " + sig);
    }
  }

}
