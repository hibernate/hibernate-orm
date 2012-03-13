/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.derivedidentities.e1.b.embeddedidcount;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

@NamedQueries({
    @NamedQuery(name = PurchaseOrder.QUERY_ALL,
            query = "SELECT p FROM PurchaseOrder p"),
        /*
    @NamedQuery(name = PurchaseOrder.QUERY_POPULAR_SUPPLIER,
            query = "SELECT p.supplier.id, COUNT(p) FROM PurchaseOrder p " +
                    "WHERE p.site = :site AND p.startDate BETWEEN :startTime " +
                    "AND :endTime GROUP BY p.supplier.id") ,
                    */
    @NamedQuery(name = PurchaseOrder.QUERY_COUNT,
            query = "select COUNT(a) from PurchaseOrder a")
})
@SuppressWarnings("serial")
@Entity
@Table(name = "S_PURCH_ORDER")
@IdClass(PurchaseOrderPK.class)
public class PurchaseOrder implements Serializable {

    public static final String QUERY_ALL = "PurchaseOrder.selectAll";
    public static final String QUERY_POPULAR_SUPPLIER = "PurchaseOrder.getPopularSupplier";
    public static final String QUERY_COUNT = "PurchaseOrder.count";

    @Id
    @TableGenerator(name = "purchaseorder",
            table = "U_SEQUENCES", 
            pkColumnName = "S_ID", 
            valueColumnName = "S_NEXTNUM", 
            pkColumnValue = "purchaseorder", 
            allocationSize = 1000)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "purchaseorder")
    @Column(name = "PO_NUMBER")
    private int id;
    
    @Id
    @Column(name = "PO_SITE_ID")
    private Integer site;

    /*
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "PO_SUPP_ID")
    private Supplier supplier;
    */
    
    @Column(name = "PO_POPULAR_SUPP")
    private int popularSupplier;
    
    @Column(name = "PO_START_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar startDate;

    @Column(name = "PO_SENT_DATE")
    @Temporal(TemporalType.DATE)
    private Calendar sentDate;

    @OneToMany(mappedBy = "purchaseOrder",
            cascade = { CascadeType.ALL })
    private List<PurchaseOrderLine> lines;

    @Version
    @Column(name = "PO_VERSION")
    private int version;

    protected PurchaseOrder() {
    }

    public PurchaseOrder(int siteId, int popularSupp) {
        //this.supplier = supplier;
        this.site = siteId;
        this.popularSupplier = popularSupp;
    }

    /**
     * Gets a PurchaseOrderLine matching the given lineNumber. 
     * Otherwise null.
     */
    public PurchaseOrderLine getOrderLine(int lineNumber) {
        Collection<PurchaseOrderLine> lines = this.getLines();
        for (PurchaseOrderLine line : lines) {
            if (line.getPk().getLineNumber() == lineNumber) {
                return line;
            }
        }
        return null;
    }

    /**
     * Create a PurchaseOrderLine and add it to this receiver.
     * The line number of the newly created PurchaseOrderLine is one more of
     * the number of the last PurchaseOrderLines attached to this receiver.
     */   
    public PurchaseOrderLine addLine(String componentId, int quantity,
            int location, BigDecimal balance, int leadTime, String message) {
        if (lines == null)
            lines = new ArrayList<PurchaseOrderLine>();
        int serial = (lines.isEmpty()) 
            ? 1 : lines.get(lines.size()-1).getPk().getLineNumber() + 1;
        PurchaseOrderLine orderLine =
                new PurchaseOrderLine(this, serial, componentId,
                        quantity, location, balance, leadTime, message);
        lines.add(orderLine);
        return orderLine;
    }

    /**
     * Gets all the PurchaseOrderLine attached to this receiver.
     */
    public List<PurchaseOrderLine> getLines() {
        return lines;
    }

    public int getId() {
        return id;
    }

    public int getSite() {
        return site;
    }

    public void setSite(int poSiteID) {
        this.site = poSiteID;
    }

    /**
     * Gets the immutable Supplier of this receiver.
     */
    /*
    public Supplier getSupplier() {
        return supplier;
    }
    */

    public void setPopularSupplier(int popularSupp) {
        this.popularSupplier = popularSupp;
    }

    public int getPopularSupplier() {
        return popularSupplier;
    }

    public Calendar getStartDate() {
        return startDate;
    }

    public void setStartDate(Calendar instance) {
        startDate = instance;
    }

    public Calendar getSentDate() {
        return sentDate;
    }

    public void setSentDate(Calendar sentDate) {
        this.sentDate = sentDate;
    }

    public int getVersion() {
        return version;
    }

    public void markSent() {
        this.setSentDate(Calendar.getInstance());
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof PurchaseOrder))
            return false;
        
        return id == ((PurchaseOrder) obj).id;
    }
}
