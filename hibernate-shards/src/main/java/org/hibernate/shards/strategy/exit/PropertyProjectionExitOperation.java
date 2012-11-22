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
import org.hibernate.criterion.PropertyProjection;
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

public class PropertyProjectionExitOperation implements ExitOperation {

    private final PropertyProjection propertyProjection;

    public PropertyProjectionExitOperation(final PropertyProjection propertyProjection) {
        this.propertyProjection = propertyProjection;
    }

    @Override
    public List<Object> apply(final List<Object> result) {

        final List<Object> projectedList = new ArrayList<Object>();

        for (final Object o : result) {
            //If there were Orders in the criteria we will get an array
            //else we will get an object
            if (o.getClass().isArray()) {
                Object[] array = (Object[]) o;
                projectedList.add(array[0]);
            } else {
                projectedList.add(o);
            }
        }

        return projectedList;
    }
}
