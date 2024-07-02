package org.hibernate.orm.test.query.hhh18291;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "paid_invoice")
public class PaidInvoiceBE {

  @Id
//  @GeneratedValue
  private long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private InvoiceBE invoice;

  public PaidInvoiceBE setId(long id) {
    this.id = id;
    return this;
  }

  public PaidInvoiceBE setInvoice(InvoiceBE invoice) {
    this.invoice = invoice;
    return this;
  }
}
