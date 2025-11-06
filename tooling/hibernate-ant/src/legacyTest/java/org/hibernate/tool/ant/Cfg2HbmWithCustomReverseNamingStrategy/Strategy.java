/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.Cfg2HbmWithCustomReverseNamingStrategy;

import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.core.strategy.AbstractStrategy;

public class Strategy extends AbstractStrategy {

	public String tableToClassName(TableIdentifier tableIdentifier) {
		return "foo.Bar";
	}

}
