/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.Cfg2HbmWithPackageNameAndReverseNamingStrategy;

import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.core.strategy.AbstractStrategy;

public class Strategy extends AbstractStrategy {

	public String columnToPropertyName(TableIdentifier table, String columnName) {
	if ("NAME".equals(columnName)) {
		return "barName";
	}
	else {
		return super.columnToPropertyName(table, columnName);
	}

	}

}
