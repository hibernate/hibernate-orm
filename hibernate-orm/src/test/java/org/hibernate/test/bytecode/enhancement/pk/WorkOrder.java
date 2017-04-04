/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.bytecode.enhancement.pk;

import java.util.Calendar;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

@SuppressWarnings("serial")
@Entity
@IdClass(WorkOrderPK.class)
public class WorkOrder {

    @Id
    private int id;

    @Id
    private int location;


    private int originalQuantity;

    private int completedQuantity;

    @Temporal(TemporalType.TIMESTAMP)
    private Calendar dueDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Calendar startDate;

    private String assemblyId;

    @Version
    private int version;

    public WorkOrder() {
    	this("", 1, 0, Calendar.getInstance());
    }

    public WorkOrder(String assemblyId, int origQty, int location, Calendar dueDate) {
        if (origQty < 1)
            throw new IllegalArgumentException("WorkOrder can not be created with original quantity " + origQty + ". Must be > 0");
        if (dueDate == null)
            throw new IllegalArgumentException("WorkOrder can not be created with null due Date");
        this.assemblyId = assemblyId;
        this.originalQuantity = origQty;
        this.dueDate = dueDate;
        this.location=location;
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
    	this.id = id;
    }

    // --- //
    
    public String getAssemblyId() {
    	return assemblyId;
    }
    
    public int getCompletedQuantity() {
        return completedQuantity;
    }

    public void setCompletedQuantity(int compQty) {
        this.completedQuantity = compQty;
    }

    public Calendar getDueDate() {
        return dueDate;
    }

    public int getOriginalQuantity() {
        return originalQuantity;
    }

    public int getLocation() {
      return location;
    }

    public Calendar getStartDate() {
        return startDate;
    }

    public int getVersion() {
        return version;
    }

    // --- //

    public void setStartDate(Calendar instance) {
        startDate = instance;
    }

    public void setLocation(int location) {
        this.location = location;
    }

    public void setDueDate(Calendar dueDate) {
        this.dueDate = dueDate;
    }

    public void setAssemblyId(String assemblyId) {
        this.assemblyId = assemblyId;
    }

    public void setOriginalQuantity(int originalQuantity) {
        this.originalQuantity = originalQuantity;
    }

    // Processing methods

    /**
     * Moves to the next state of processing. 
     * Return true if the new status can be updated again.
     */
    public boolean update() {
        return true;
    }

    /**
     * When workOrder is finished, it will add the new object to inventory and
     * modify the state of workOrder to finished.
     */
    public boolean setStatusCompleted() {
        return true;
    }
    
    public void advanceStatus() {
    }
    
    public void setStatusCancelled() {
    }

    // --- //
    
    public boolean equals(Object other) {
        if (this == other) 
            return true;
        if (other == null || !(other instanceof WorkOrder))
            return false;
        return id == ((WorkOrder)other).id;
    }
    
    public int hashCode() {
        final int PRIME = 31;
        return PRIME * new Integer(id).hashCode();
    }

    public String toString() {
        return "WorkOrder:["+ getId() + "]" ;
    }

}
