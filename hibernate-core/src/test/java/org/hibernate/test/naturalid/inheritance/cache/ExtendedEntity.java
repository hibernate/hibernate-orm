package org.hibernate.test.naturalid.inheritance.cache;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
public class ExtendedEntity extends MyEntity {
  public ExtendedEntity() {
  }

  public ExtendedEntity(final String uid, final String extendedValue) {
    super(uid);
    this.extendedValue = extendedValue;
  }

  private String extendedValue;

  @Column
  public String getExtendedValue() {
    return extendedValue;
  }

  public void setExtendedValue(final String extendedValue) {
    this.extendedValue = extendedValue;
  }
}
