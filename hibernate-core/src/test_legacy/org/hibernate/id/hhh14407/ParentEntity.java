/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.hhh14407;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Version;

/**
 * @author Sönke Reimer
 */
@Entity(name="ParentEntity")
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
class ParentEntity {

  @Id
  @Column(name = "ID", length = 32)
  private String Id;

  @Version
  @Column(name = "LOCK_VERSION")
  private int Lock_Version;
  public String getId() {
    return Id;
  }
  
}
