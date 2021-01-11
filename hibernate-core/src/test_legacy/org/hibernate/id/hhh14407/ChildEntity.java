/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.hhh14407;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * @author Sönke Reimer
 */
@Entity(name="ChildEntity")
class ChildEntity extends ParentEntity {

  @Basic
  @Column(name="CHILD")
  private String ivChild;

}
