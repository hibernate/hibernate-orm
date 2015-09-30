/*
 *   SPECjEnterprise2010 - a benchmark for enterprise middleware
 *  Copyright 1995-2010 Standard Performance Evaluation Corporation
 *   All Rights Reserved
 *
 *  History:
 *  Date        ID, Company             Description
 *  ----------  ----------------        ----------------------------------------------
 *  2009/05/31  Anoop Gupta, Oracle     Created for SPECjEnterprise2010
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
