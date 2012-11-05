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
package org.hibernate.shards.integration.model;

import org.hibernate.HibernateException;
import org.hibernate.shards.integration.BaseShardingIntegrationTestCase;
import org.hibernate.shards.model.IdIsBaseType;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ModelIntegrationTest extends BaseShardingIntegrationTestCase {

  public void testSaveIdIsBaseType() {
    IdIsBaseType hli = new IdIsBaseType();
    session.beginTransaction();
    hli.setValue("yamma");
    session.save(hli);
    commitAndResetSession();
    hli = reload(hli);
    assertNotNull(hli);
  }

  public void testSaveOrUpdateIdIsBasetype() {
    IdIsBaseType hli = new IdIsBaseType();
    session.beginTransaction();
    hli.setValue("yamma");
    session.saveOrUpdate(hli);
    commitAndResetSession();
    hli = reload(hli);
    assertNotNull(hli);
  }

  public void testUpdateIdIsBasetype() {
    IdIsBaseType hli = new IdIsBaseType();
    session.beginTransaction();
    hli.setValue("yamma");
    session.update(hli);
    try {
      session.getTransaction().commit();
      fail("expected he");
    } catch (HibernateException he) {
      // good
    }
    resetSession();
    session.beginTransaction();
    session.saveOrUpdate(hli);
    commitAndResetSession();
    hli = reload(hli);
    assertNotNull(hli);
  }
}
