package org.hibernate.test.naturalid.inheritance.cache;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;

@Entity
@Table
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class MyEntity {
  private Long id;
  private String uid;

  public MyEntity() {
  }

  public MyEntity(String uid) {
    this.uid = uid;
  }

  @Id
  @GeneratedValue(generator = "increment")
  @GenericGenerator(name = "increment", strategy = "increment")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @NaturalId
  @Column
  public String getUid() {
    return uid;
  }
  
  public void setUid(final String uid) {
    this.uid = uid;
  }
}
