/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.xml;

public interface XMLPrettyPrinterStrategy {

	String prettyPrint(String xml) throws Exception;
}
