/*
 *   SPECjEnterprise2010 - a benchmark for enterprise middleware
 *  Copyright 1995-2010 Standard Performance Evaluation Corporation
 *   All Rights Reserved
 */

package org.hibernate.test.bytecode.enhancement.pk;

import java.util.Calendar;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;

/**
 * Represent a work order that evolves through multiple processing stages. 
 *
 */
@SuppressWarnings("serial")

@NamedQueries({
        @NamedQuery(name=WorkOrder.QUERY_ALL, 
                   query="select w from WorkOrder w"),
//        @NamedQuery(name=WorkOrder.QUERY_BY_STATUS,
//                   query="select w from WorkOrder w where w.status = :status"),
        @NamedQuery(name=WorkOrder.QUERY_BY_OID_OLID,
                   query="select w from WorkOrder w where w.location = :location and w.salesId = :salesId and w.orderLineId = :orderLineId"),
        @NamedQuery(name=WorkOrder.QUERY_COUNT,
                   query="select COUNT(a) from WorkOrder a")
})

@Entity
@Table(name = "M_WORKORDER")
@XmlAccessorType(XmlAccessType.PROPERTY)
@IdClass(WorkOrderPK.class)
public class WorkOrder {

    public static final String QUERY_ALL = "WorkOrder.selectAll";
    //public static final String QUERY_BY_STATUS = "WorkOrder.selectByStatus";
    public static final String QUERY_BY_OID_OLID = "WorkOrder.selectByOID_OLID";
    public static final String QUERY_COUNT = "WorkOrder.count";

    @Id
    @TableGenerator(name = "workorder", 
            table = "U_SEQUENCES", 
            pkColumnName = "S_ID", 
            valueColumnName = "S_NEXTNUM", 
            pkColumnValue = "workorder", 
            allocationSize = 1000)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "workorder")
    @Column(name = "WO_NUMBER")
    private int id;

    @Id
    @Column(name = "WO_LOCATION")
    private int location;
    
    @Column(name = "WO_O_ID")
    private int salesId;

    @Column(name = "WO_OL_ID")
    private int orderLineId;

    @Column(name = "WO_ORIG_QTY")
    private int originalQuantity;

    @Column(name = "WO_COMP_QTY")
    private int completedQuantity;

    @Column(name = "WO_DUE_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar dueDate;

    @Column(name = "WO_START_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar startDate;

    @Column(name = "WO_ASSEMBLY_ID")
    private String assemblyId;
    
    @Version
    @Column(name = "WO_VERSION")
    private int version;

    /**
     * Public no-arg constructor required by JAXB specification.
     */
    public WorkOrder() {
    	this("", 1, 0, Calendar.getInstance());
    }
    
    /**
     * Construct with proper state. The status at construction is OPEN.
     * @param location
     */
    public WorkOrder(String assemblyId, int origQty, int location, Calendar dueDate) {
        if (origQty < 1)
            throw new IllegalArgumentException("WorkOrder can not be created " +
                    " with original quantity " + origQty + ". Must be > 0");
        if (dueDate == null)
            throw new IllegalArgumentException("WorkOrder can not be created " +
                    " with null due Date");
        this.assemblyId = assemblyId;
        originalQuantity = origQty;
        this.dueDate = dueDate;
        this.location=location;
    }

    /**
     * Construct with proper state. The status at construction is OPEN.
     * @param location
     */
    public WorkOrder(String assemblyId, int salesId, int oLineId, int origQty,
            int location, Calendar dueDate) {
        this(assemblyId, origQty, location, dueDate);        
        this.salesId = salesId; 
        orderLineId = oLineId;
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
    	this.id = id;
    }
    
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

    public int getOrderLineId() {
        return orderLineId;
    }

    public int getOriginalQuantity() {
        return originalQuantity;
    }
   
    public int getSalesId() {
        return salesId;
    }
    
    public int getLocation() {
      return location;
    }

    @XmlSchemaType(name = "dateTime")
    public Calendar getStartDate() {
        return startDate;
    }

    public int getVersion() {
        return version;
    }

    // ======================================================================
    // Processing methods
    // ======================================================================
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

	public void setSalesId(int salesId) {
		this.salesId = salesId;
	}

	public void setOrderLineId(int orderLineId) {
		this.orderLineId = orderLineId;
	}

}
