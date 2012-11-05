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

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.TableHiLoGenerator;
import org.hibernate.shards.session.ControlSessionProvider;

import java.io.Serializable;

/**
 * TableHiLoGenerator which uses control shard to store table with hi values.
 *
 * @see org.hibernate.id.TableHiLoGenerator
 * @author Tomislav Nad
 */

public class ShardedTableHiLoGenerator extends TableHiLoGenerator implements GeneratorRequiringControlSessionProvider {

  private ControlSessionProvider controlSessionProvider;

  @Override
  public Serializable generate(SessionImplementor session, Object obj)
      throws HibernateException {
    Serializable id;
    SessionImplementor controlSession = null;
    try {
      controlSession = controlSessionProvider.openControlSession();
      id = superGenerate(controlSession, obj);
    } finally {
      if (controlSession != null) {
        ((Session)controlSession).close();
      }
    }
    return id;
  }

  public void setControlSessionProvider(ControlSessionProvider provider) {
    this.controlSessionProvider = provider;
  }

  Serializable superGenerate(SessionImplementor controlSession, Object obj) {
    return super.generate(controlSession, obj);
  }
}
