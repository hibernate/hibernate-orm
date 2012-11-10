/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.strategy.exit;

import org.hibernate.criterion.Order;
import org.hibernate.shards.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author "Aviad Lichtenstadt"
 *         <p/>
 *         A special Order exit operation for cases of property projection in this case
 *         we just go over the array
 */

public class PropertyProjectionOrderExitOperation implements ExitOperation {

    private final List<OrderProperty> orderProperties = new ArrayList<OrderProperty>();

    public PropertyProjectionOrderExitOperation(final List<Order> orders) {
        for (final Order order : orders) {
            // TODO(maulik) support Ignore case!
            Preconditions.checkState(order.toString().endsWith("asc")
                    || order.toString().endsWith("desc"));

            orderProperties.add(new OrderProperty(order.toString().endsWith("asc")));
        }
    }

    @Override
    public List<Object> apply(final List<Object> results) {
        final List<Object> nonNullList = ExitOperationUtils.getNonNullList(results);
        final Comparator<Object> comparator = new Comparator<Object>() {

            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {

                if (o1 == o2) {
                    return 0;
                }
                int cmp = 0;

                // the object must be array object other wise we wont get to this piece of code
                Object[] arr1 = (Object[]) o1;
                Object[] arr2 = (Object[]) o2;
                int i = 0;

                for (i = 1; i < arr1.length; i++) {
                    Comparable<Object> o1Value = (Comparable<Object>) arr1[i];
                    if (arr2.length <= i) {
                        cmp = 1;
                        break;
                    }

                    Comparable<Object> o2Value = (Comparable<Object>) arr2[i];
                    if (o1Value == null) {
                        cmp = -1;
                        break;
                    }

                    cmp = o1Value.compareTo(o2Value);

                    if (cmp == 0) {
                        break;
                    }
                }

                if (cmp != 0 && !orderProperties.get(i - 1).isAsc()) {
                    cmp = cmp * (-1);
                }

                return cmp;
            }
        };

        Collections.sort(nonNullList, comparator);

        return nonNullList;
    }

    private class OrderProperty {
        private final boolean asc;

        private OrderProperty(boolean asc) {
            this.asc = asc;
        }

        public boolean isAsc() {
            return asc;
        }
    }
}