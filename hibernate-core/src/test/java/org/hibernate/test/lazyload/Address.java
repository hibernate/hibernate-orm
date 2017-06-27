package org.hibernate.test.lazyload;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * @author Igor Dmitriev
 */

@Entity
public class Address {
  @Id
  @GeneratedValue
  private Long id;

  @Column
  private String street;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_client")
  private Client client;

  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Client getClient() {
    return client;
  }

  public void setClient(Client client) {
    this.client = client;
  }
}
