package org.hibernate.test.model.orders.old;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Access(AccessType.FIELD)
@Table(name="purchase_old ")
public class Purchase {
  @Id
  public String id;
}
