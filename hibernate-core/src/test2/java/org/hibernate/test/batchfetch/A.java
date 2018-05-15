/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.batchfetch;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class A {
  @Id
  @GeneratedValue
  private Integer id;

  private String otherProperty;

  @OneToOne(fetch = FetchType.LAZY)
  private B b;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getOtherProperty() {
    return otherProperty;
  }

  public void setOtherProperty(String otherProperty) {
    this.otherProperty = otherProperty;
  }

  public B getB() {
    return b;
  }

  public void setB(B b) {
    this.b = b;
  }
}
