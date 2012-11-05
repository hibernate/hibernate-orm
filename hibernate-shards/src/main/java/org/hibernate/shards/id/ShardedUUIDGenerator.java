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

package org.hibernate.shards.id;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.session.ShardedSessionImpl;
import org.hibernate.shards.util.Preconditions;
import org.hibernate.type.Type;
import org.hibernate.util.PropertiesHelper;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Properties;

/**
 * Supports generation of either 32-character hex String UUID or 128 bit
 * BigInteger UUID that encodes the shard.
 *
 * @author Tomislav Nad
 */
public class ShardedUUIDGenerator extends UUIDHexGenerator implements ShardEncodingIdentifierGenerator {

  private IdType idType;

  private static String ZERO_STRING = "00000000000000000000000000000000";
  private static String ID_TYPE_PROPERTY = "sharded-uuid-type";

  private static enum IdType { STRING, INTEGER }

  private int getShardId() {
    ShardId shardId = ShardedSessionImpl.getCurrentSubgraphShardId();
    Preconditions.checkState(shardId != null);
    return shardId.getId();
  }

  public ShardId extractShardId(Serializable identifier) {
    Preconditions.checkNotNull(identifier);
    String hexId;
    switch(idType) {
      case STRING:
        hexId = (String)identifier;
        return new ShardId(Integer.decode("0x" + hexId.substring(0, 4)));
      case INTEGER:
        String strippedHexId = ((BigInteger)identifier).toString(16);
        hexId = ZERO_STRING.substring(0, 32 - strippedHexId.length()) + strippedHexId;
        return new ShardId(Integer.decode("0x" + hexId.substring(0, hexId.length()-28)));
      default:
        // should never get here
        throw new IllegalStateException("ShardedUUIDGenerator was not configured properly");
    }
  }

  @Override
  public Serializable generate(SessionImplementor session, Object object) {
    String id =  new StringBuilder(32).append(format((short)getShardId()))
                                      .append(format(getIP()))
                                      .append(format((short)(getJVM()>>>16)))
                                      .append(format(getHiTime()))
                                      .append(format(getLoTime()))
                                      .append(format(getCount()))
                                      .toString();
    switch(idType) {
      case STRING:
        return id;
      case INTEGER:
        return new BigInteger(id, 16);
      default:
        // should never get here
        throw new IllegalStateException("ShardedUUIDGenerator was not configured properly");
    }
  }

  @Override
  public void configure(Type type, Properties params, Dialect d) {
    this.idType = IdType.valueOf(PropertiesHelper.getString(ID_TYPE_PROPERTY, params, "INTEGER"));
  }

}
