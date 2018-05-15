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
public class Resource {

   java.lang.String id;
   java.lang.String name;
   java.lang.String userCode;


  java.lang.String getId() {
    return id;
  }

  void  setId(java.lang.String newValue) {
    id = newValue;
  }

  java.lang.String getName() {
    return name;
  }

  void  setName(java.lang.String newValue) {
    name = newValue;
  }

  java.lang.String getUserCode() {
    return userCode;
  }

  void  setUserCode(java.lang.String newValue) {
    userCode = newValue;
  }


}
