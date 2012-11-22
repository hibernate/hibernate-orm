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

package org.hibernate.shards.criteria;

import org.hibernate.Criteria;
import org.hibernate.criterion.AggregateProjection;
import org.hibernate.criterion.Projection;

/**
 * Event that allows a Projection to be lazily added to a Criteria.
 *
 * @author aviadl@sentrigo.com (Aviad Lichtenstadt)
 * @see org.hibernate.Criteria#setProjection(org.hibernate.criterion.Projection)
 */
class GeneralProjectionEvent implements CriteriaEvent {

    // the Projection we're going to add when the event fires
    private final Projection projection;

    public GeneralProjectionEvent(Projection projection) {
        this.projection = projection;
    }

    @Override
    public void onEvent(final Criteria crit) {
        crit.setProjection(projection);
    }
}
