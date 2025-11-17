/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

/**
 * @author Steve Ebersole
 */
interface ColumnAndFormulaTarget {
	TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults);

	void addColumn(TargetColumnAdapter column);

	void addFormula(String formula);
}
