/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;


/**
 * @author hbm2java
 */
public class Drug extends org.hibernate.test.legacy.Resource {

   java.lang.String id;


  java.lang.String getId() {
    return id;
  }

  void  setId(java.lang.String newValue) {
    id = newValue;
  }


}
