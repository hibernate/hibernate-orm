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

package org.hibernate.shards.session;

import org.hibernate.Interceptor;

/**
 * Interface describing an object that knows how to create Interceptors.
 * Technically this is just an interceptor factory, but it is designed
 * to be used by clients who want to use stateful interceptors in conjunction
 * with sharded sessions.  Clients should make sure their Interceptor
 * implementation implements this interface.  Furthermore, if the
 * Interceptor implementation requires a reference to the Session, the
 * Interceptor returned by newInstance() should implement the {@link RequiresSession}
 * interface.
 *
 * @author maxr@google.com (Max Ross)
 */
public interface StatefulInterceptorFactory {
  Interceptor newInstance();
}
