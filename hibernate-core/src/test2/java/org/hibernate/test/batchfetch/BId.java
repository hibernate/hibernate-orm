/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.batchfetch;

import java.io.Serializable;

public class BId
    implements Serializable {
  private static final long serialVersionUID = 1L;

  private Integer idPart1;
  private Integer idPart2;

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

}