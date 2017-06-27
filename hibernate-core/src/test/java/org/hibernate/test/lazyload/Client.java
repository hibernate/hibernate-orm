package org.hibernate.test.lazyload;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * @author Igor Dmitriev
 */

@Entity
public class Client {
  @Id
  @GeneratedValue
  private Long id;

  @Column
  private String name;

  @OneToMany(mappedBy = "client")
  private List<Account> accounts = new ArrayList<>();

  @OneToOne(mappedBy = "client", fetch = FetchType.LAZY)
  private Address address;

  public Client() {
  }

  public Client(Address address) {
    this.address = address;
    address.setClient(this);
  }

  public void addAccount(Account account) {
    accounts.add(account);
    account.setClient(this);
  }

  public String getName() {
    return name;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }
}
