/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.batchfetch;

import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(BId.class)
@BatchSize(size = 1000)
public class B {

  @Id
  private Integer idPart1;

  @Id
  private Integer idPart2;

  private String otherProperty;

  public Integer getIdPart1() {
    return idPart1;
  }

  public void setIdPart1(Integer idPart1) {
    this.idPart1 = idPart1;
  }

  public Integer getIdPart2() {
    return idPart2;
  }

  public void setIdPart2(Integer idPart2) {
    this.idPart2 = idPart2;
  }

  public String getOtherProperty() {
    return otherProperty;
  }

  public void setOtherProperty(String otherProperty) {
    this.otherProperty = otherProperty;
  }
}
