package org.hibernate.boot.jaxb.hbm.transform;

import java.io.Serializable;
import java.util.List;

/**
 * @author Steve Ebersole
 */
interface ColumnAndFormulaSource {
	String getColumnAttribute();

	String getFormulaAttribute();

	List<Serializable> getColumnOrFormula();

	SourceColumnAdapter wrap(Serializable column);
}
