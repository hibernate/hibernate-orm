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
public class Medication extends org.hibernate.test.legacy.Intervention {

   org.hibernate.test.legacy.Drug prescribedDrug;

  
  org.hibernate.test.legacy.Drug getPrescribedDrug() {
    return prescribedDrug;
  }

  void  setPrescribedDrug(org.hibernate.test.legacy.Drug newValue) {
    prescribedDrug = newValue;
  }


}
