/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
