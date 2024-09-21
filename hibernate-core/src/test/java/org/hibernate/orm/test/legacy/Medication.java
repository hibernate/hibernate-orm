/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
