/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author hbm2java
 */
public class Role {

   long id;
   java.lang.String name;
   Set interventions = new HashSet();
private List bunchOfStrings;

  long getId() {
    return id;
  }

  void  setId(long newValue) {
    id = newValue;
  }

  java.lang.String getName() {
    return name;
  }

  void  setName(java.lang.String newValue) {
    name = newValue;
  }

  public Set getInterventions() {
  	return interventions;
  }
  
  public void setInterventions(Set iv) {
  	interventions = iv;
  }

  List getBunchOfStrings() {
  	return bunchOfStrings;
  }
  
  void setBunchOfStrings(List s) {
  	bunchOfStrings = s;
  }
}
