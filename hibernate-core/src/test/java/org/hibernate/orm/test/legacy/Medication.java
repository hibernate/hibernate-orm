/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


/**
 * @author hbm2java
 */
public class Medication extends Intervention {

Drug prescribedDrug;


Drug getPrescribedDrug() {
	return prescribedDrug;
}

void  setPrescribedDrug(Drug newValue) {
	prescribedDrug = newValue;
}


}
