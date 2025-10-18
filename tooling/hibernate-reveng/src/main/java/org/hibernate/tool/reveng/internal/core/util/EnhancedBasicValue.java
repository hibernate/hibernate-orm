/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.util;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Table;

import java.util.Properties;

@SuppressWarnings("serial")
public class EnhancedBasicValue extends BasicValue implements EnhancedValue {

	private Properties idGenProps = new Properties();
	private String genStrategy = null;

	public EnhancedBasicValue(MetadataBuildingContext buildingContext, Table table) {
		super(buildingContext, table);
	}

	@Override
	public void setIdentifierGeneratorProperties(Properties props) {
		idGenProps = props;

	}

	@Override
	public Properties getIdentifierGeneratorProperties() {
		return idGenProps;
	}

	@Override
	public void setIdentifierGeneratorStrategy(String s) {
		genStrategy = s;
	}

	@Override
	public String getIdentifierGeneratorStrategy() {
		return genStrategy;
	}

}
