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
public class Intervention {

   java.lang.String id;
   long version;

   String description;

  java.lang.String getId() {
    return id;
  }

  void  setId(java.lang.String newValue) {
    id = newValue;
  }

  long getVersion() {
    return version;
  }

  void  setVersion(long newValue) {
    version = newValue;
  }


public String getDescription() {
	return description;
}
public void setDescription(String description) {
	this.description = description;
}
}
