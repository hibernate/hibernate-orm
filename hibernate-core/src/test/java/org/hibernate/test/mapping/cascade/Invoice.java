package org.hibernate.test.mapping.cascade;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cascade;

/**
 * Invoice with invoice lines as an example for a relationship between components
 */
@Entity
@Table(name = "OTM_Invoice")
public class Invoice {

    @OneToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @Cascade(value = {org.hibernate.annotations.CascadeType.SAVE_UPDATE, org.hibernate.annotations.CascadeType.DELETE_ORPHAN})
    @JoinColumn(name = "INVOICE_ID", nullable = false)
    private final Set<InvoiceLine> invoiceLines = new HashSet<InvoiceLine>();
    
    @Column(length = 50, nullable = false)
    private String name;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @AccessType("property")
    private Long id;
    
    /**
     * Constructor
     */
    public Invoice() {
        super();
    }

    /**
     * Constructor
     * @param name
     */
    public Invoice(String name) {
        super();
        this.name = name;
    }

    /**
     * @return invoiceLines
     */
    public Set<InvoiceLine> getInvoiceLines() {
        return invoiceLines;
    }

    /**
     * @param someInvoiceLines
     */
    public void setInvoiceLines(Collection<InvoiceLine> someInvoiceLines) {
        invoiceLines.clear();
        invoiceLines.addAll(someInvoiceLines);
    }

    /**
     * @param invoiceLine
     */
    public void addInvoiceLine(InvoiceLine invoiceLine) {
        invoiceLines.add(invoiceLine);
    }

    /**
     * @param invoiceLine
     */
    public void removeInvoiceLine(InvoiceLine invoiceLine) {
        invoiceLines.remove(invoiceLine);
    }

    /**
     * @param someInvoiceLines
     */
    public void addInvoiceLines(Collection<InvoiceLine> someInvoiceLines) {
        invoiceLines.addAll(someInvoiceLines);
    }

    /**
     * @param someInvoiceLines
     */
    public void removeInvoiceLines(Collection<InvoiceLine> someInvoiceLines) {
        invoiceLines.removeAll(someInvoiceLines);
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
    * @return id
    */
    public Long getId() {
        return id;
    }

    /**
    * @param id
    */
    public void setId(Long id) {
        this.id = id;
    }
}