/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.xml.hbm;


/**
 * @author Emmanuel Bernard
 */
public interface Z extends java.io.Serializable {
  public Integer getZId();

  public void setZId(Integer zId);

  public B getB();

  public void setB(B b);
}
