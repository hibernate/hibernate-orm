/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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