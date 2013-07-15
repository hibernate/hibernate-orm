package org.hibernate.test.annotations.derivedidentities.bidirectional;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

@Entity
@NamedQuery(name="PersonQuery", query="SELECT p FROM Person p")
public class Person
  implements Serializable
{
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  private Integer id;

  @Basic
  private String name;

  @OneToOne(mappedBy="id")
  private PersonInfo personInfo;

  public Integer getId()
  {
    return this.id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int hashCode()
  {
    int hash = 0;
    hash += (this.id != null ? this.id.hashCode() : 0);
    return hash;
  }

  public boolean equals(Object object)
  {
    if (!(object instanceof Person)) {
      return false;
    }
    Person other = (Person)object;

    return ((this.id != null) || (other.id == null)) && ((this.id == null) || (this.id.equals(other.id)));
  }

  public String toString()
  {
    return "nogroup.hibertest.Person[ id=" + this.id + " ]";
  }

  public PersonInfo getPersonInfo()
  {
    return this.personInfo;
  }

  public void setPersonInfo(PersonInfo personInfo)
  {
    this.personInfo = personInfo;
  }
}
