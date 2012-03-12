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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.persistence.*;

@NamedQueries({
    @NamedQuery(name = PurchaseOrderLine.QUERY_COUNT,
            query = "select COUNT(a) from PurchaseOrderLine a")
})
@Entity
@Table(name = "S_PURCH_ORDERLINE")
@SuppressWarnings("serial")
public class PurchaseOrderLine implements Serializable {

    public static final String QUERY_COUNT = "PurchaseOrderLine.count";

    @EmbeddedId
    @AttributeOverrides({
        @AttributeOverride(name = "lineNumber", column = @Column(name = "POL_NUMBER")),
        @AttributeOverride(name = "poID", column = @Column(name = "POL_PO_ID")),
        @AttributeOverride(name = "location", column = @Column(name = "POL_LOCATION"))
    })
    private PurchaseOrderLinePK pk;
    @Column(name = "POL_P_ID")
    protected String componentId; // id of the component being ordered
    @Column(name = "POL_QTY")
    protected int quantity;
    @Column(name = "POL_BALANCE")
    protected BigDecimal balance;
    @Column(name = "POL_DELDATE")
    @Temporal(TemporalType.DATE)
    protected Calendar deliveryDate;
    @Column(name = "POL_MESSAGE")
    protected String message;
    @Column(name = "POL_LEADTIME")
    private int leadTime;

    @ManyToOne
    @JoinColumns({
    	@JoinColumn(name = "POL_LOCATION", referencedColumnName = "PO_SITE_ID", updatable = false, insertable = false),
    	@JoinColumn(name = "POL_PO_ID", referencedColumnName = "PO_NUMBER", updatable = false, insertable = false)
    })
    protected PurchaseOrder purchaseOrder;

    @Version
    @Column(name = "POL_VERSION")
    protected int version;

    /**
     * no-arg constructor required by JPA Specification.
     */
    protected PurchaseOrderLine() {
    }

    /**
     * Restricted access to allow only PurchaseOrder to construct its line item.
     */
    PurchaseOrderLine(PurchaseOrder purchaseOrder, int lineNumber,
            String componentId, int quantity, int location,
            BigDecimal balance, int leadTime, String message) {
        this.pk = new PurchaseOrderLinePK(lineNumber, purchaseOrder.getId(), location);
        this.purchaseOrder = purchaseOrder;
        this.componentId = componentId;
        this.quantity = quantity;
        this.balance = balance;
        this.message = message;
        this.leadTime = leadTime;
    }

    public PurchaseOrderLinePK getPk() {
        return pk;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Calendar getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Calendar deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getComponentId() {
        return componentId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String poLineMsg) {
        this.message = poLineMsg;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int poLineQty) {
        this.quantity = poLineQty;
    }

    public int getLeadTime() {
        return leadTime;
    }

    public void setLeadTime(int leadTime) {
        this.leadTime = leadTime;
    }

    public int getVersion() {
        return version;
    }

    public void markDelivered() {
        this.setDeliveryDate(Calendar.getInstance());
    }
}
