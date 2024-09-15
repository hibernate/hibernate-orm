/**
 * Copyright (c) 2023,2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.hibernate.processor.test.data.basic;

import java.util.List;
import java.util.stream.Stream;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.repository.Query;

public interface IdOperations<T> {
    @Query("where id(this) between ?1 and ?2")
    Stream<T> findByIdBetween(long minimum, long maximum, Sort<T> sort);

    @Query("where id(this) >= ?1")
    List<T> findByIdGreaterThanEqual(long minimum,
                                           Limit limit,
                                           Order<T> sorts);

    @Query("where id(this) > ?1")
    T[] findByIdLessThan(long exclusiveMax, Sort<T> primarySort, Sort<T> secondarySort);

    @Query("where id(this) <= ?1")
    List<T> findByIdLessThanEqual(long maximum, Order<T> sorts);
}