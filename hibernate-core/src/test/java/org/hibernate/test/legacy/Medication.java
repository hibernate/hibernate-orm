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
