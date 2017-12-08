/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.envers;

import org.hibernate.envers.strategy.DefaultAuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;

/**
 * @author Chris Cranford
 */
public enum Strategy {
	DEFAULT( "<straetgy:default>", null, DefaultAuditStrategy.class ),
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
