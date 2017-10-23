/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.bytecode.enhancement.pk;

import java.io.Serializable;

@SuppressWarnings("serial")
public class WorkOrderPK implements Serializable {
    private int id;
    private int location;

    public WorkOrderPK() {
    }

    public WorkOrderPK(int location, int id) {
        this.location = location;
        this.id = id;
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof WorkOrderPK)) {
            return false;
        }
        WorkOrderPK wop = (WorkOrderPK) other;
        return (location == wop.location && id == wop.id);
    }

    public int hashCode() {
        return id ^ location;
    }

    public int getId() {
        return id;
    }

    public int getLocation() {
        return location;
    }

}
