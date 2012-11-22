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

package org.hibernate.shards.integration;

/**
 * @author maxr@google.com (Max Ross)
 */
public enum IdGenType {
  SIMPLE("mappings.hbm.xml", false),
  SHARD_HI_LO("mappings-shardedTableHiLo.hbm.xml", false),
  SHARD_UUID("mappings-shardedUUID.hbm.xml", true);

  private final String mappingFile;
  private final boolean supportsVirtualSharding;

  private IdGenType(String mappingFile, boolean supportsVirtualSharding) {
   this.mappingFile = mappingFile;
   this.supportsVirtualSharding = supportsVirtualSharding;
 }

  public String getMappingFile() {
    return mappingFile;
  }

  public boolean getSupportsVirtualSharding() {
    return supportsVirtualSharding;
  }
}
