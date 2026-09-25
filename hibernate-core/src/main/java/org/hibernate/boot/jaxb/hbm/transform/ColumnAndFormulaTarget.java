package org.hibernate.boot.jaxb.hbm.transform;

/**
 * @author Steve Ebersole
 */
interface ColumnAndFormulaTarget {
	TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults);

	void addColumn(TargetColumnAdapter column);

	void addFormula(String formula);
}
