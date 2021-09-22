/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.hhh14407;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * @author Sönke Reimer
 */
@Entity(name="ChildEntity")
class ChildEntity extends ParentEntity {

  @Basic
  @Column(name="CHILD")
  private String ivChild;

}
