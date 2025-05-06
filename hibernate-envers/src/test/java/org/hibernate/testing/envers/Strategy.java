/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.envers;

import org.hibernate.envers.strategy.internal.DefaultAuditStrategy;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;

/**
 * @author Chris Cranford
 */
public enum Strategy {
	DEFAULT( "<strategy:default>", null, DefaultAuditStrategy.class ),
	VALIDITY( "<strategy:validity>", ValidityAuditStrategy.class.getName(), ValidityAuditStrategy.class );

	private final String displayName;
	private final String settingValue;
	private final Class<?> strategyClass;

	Strategy(String displayName, String settingValue, Class<?> strategyClass) {
		this.displayName = displayName;
		this.settingValue = settingValue;
		this.strategyClass = strategyClass;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getSettingValue() {
		return settingValue;
	}

	public boolean isStrategy(Class<?> strategyClass) {
		return strategyClass.isAssignableFrom( this.strategyClass );
	}
}
