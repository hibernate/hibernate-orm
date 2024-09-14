/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hhh18291;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoice")
public class InvoiceBE {

  @Id
//  @GeneratedValue
  private long id;

  @Column(name = "removed", nullable = false)
  private boolean removed;

  public InvoiceBE setId(long id) {
    this.id = id;
    return this;
  }

  public InvoiceBE setRemoved(boolean removed) {
    this.removed = removed;
    return this;
  }
}
