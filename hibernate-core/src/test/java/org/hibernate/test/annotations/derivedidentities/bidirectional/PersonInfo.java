package org.hibernate.test.annotations.derivedidentities.bidirectional;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class PersonInfo
  implements Serializable
{
  private static final long serialVersionUID = 1L;

  @Id
  @OneToOne
  private Person id;

  @Basic
  private String info;

  public Person getId()
  {
    return this.id;
  }

  public void setId(Person id) {
    this.id = id;
  }

  public String getInfo() {
    return this.info;
  }

  public void setInfo(String info) {
    this.info = info;
  }

  public int hashCode()
  {
    int hash = 0;
    hash += (this.id != null ? this.id.hashCode() : 0);
    return hash;
  }

  public boolean equals(Object object)
  {
    if (!(object instanceof PersonInfo)) {
      return false;
    }
    PersonInfo other = (PersonInfo)object;

    return ((this.id != null) || (other.id == null)) && ((this.id == null) || (this.id.equals(other.id)));
  }

  public String toString()
  {
    return "nogroup.hibertest.PersonInfo[ id=" + this.id + " ]";
  }
}
