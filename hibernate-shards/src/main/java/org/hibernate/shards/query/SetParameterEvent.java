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
import org.hibernate.type.Type;

/**
 * @author Maulik Shah
 */
public class SetParameterEvent implements QueryEvent {
  private static enum CtorType {
    POSITION_VAL,
    POSITION_VAL_TYPE,
    NAME_VAL,
    NAME_VAL_TYPE,
  }

  private final CtorType ctorType;

  private final int position;
  private final Object val;
  private final Type type;
  private final String name;


  private SetParameterEvent(CtorType ctorType, int position, String name, Object val, Type type) {
    this.ctorType = ctorType;
    this.position = position;
    this.val = val;
    this.type = type;
    this.name = name;
  }

  public SetParameterEvent(int position, Object val, Type type) {
    this(CtorType.POSITION_VAL_TYPE, position, null, val, type);
  }

  public SetParameterEvent(String name, Object val, Type type) {
    this(CtorType.NAME_VAL_TYPE, -1, name, val, type);
  }

  public SetParameterEvent(int position, Object val) {
    this(CtorType.POSITION_VAL, position, null, val, null);
  }

  public SetParameterEvent(String name, Object val) {
    this(CtorType.NAME_VAL, -1, name, val, null);
  }

  public void onEvent(Query query) {
    switch(ctorType) {
      case POSITION_VAL:
        query.setParameter(position, val);
        break;
      case POSITION_VAL_TYPE:
        query.setParameter(position, val, type);
        break;
      case NAME_VAL:
        query.setParameter(name, val);
        break;
      case NAME_VAL_TYPE:
        query.setParameter(name, val, type);
        break;
      default:
        throw new ShardedSessionException(
            "Unknown ctor type in SetParameterEvent: " + ctorType);
    }
  }

}
