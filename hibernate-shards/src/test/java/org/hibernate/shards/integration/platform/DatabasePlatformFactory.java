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

package org.hibernate.shards.integration.platform;

import org.hibernate.shards.integration.platform.hsql.HSQLDatabasePlatform;
import org.hibernate.shards.util.StringUtil;

/**
 * @author maxr@google.com (Max Ross)
 */
public interface DatabasePlatformFactory {
  DatabasePlatform getDatabasePlatform();

  DatabasePlatformFactory FACTORY = new DatabasePlatformFactory() {
    public DatabasePlatform getDatabasePlatform() {
      String platformClassStr = System.getProperty("hibernate.shard.database.platform");
      if(StringUtil.isEmptyOrWhitespace(platformClassStr)) {
        return getDefaultPlatform();
      }
      try {
        Class clazz = Class.forName(platformClassStr);
        return (DatabasePlatform) clazz.newInstance();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Unknown platform class", e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Could not access platform class", e);
      } catch (InstantiationException e) {
        throw new RuntimeException("Could not instantiate platform class", e);
      }
    }

    private DatabasePlatform getDefaultPlatform() {
      return HSQLDatabasePlatform.getInstance();
    }
  };
}
