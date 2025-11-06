/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.core;

public interface AssociationInfo {

		String getCascade();
		String getFetch();
		Boolean getUpdate();
		Boolean getInsert();

}
