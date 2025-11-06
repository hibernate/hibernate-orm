/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.export;

import java.util.Properties;

/**
 * @author max and david
 * @author koen
 */
public interface Exporter {


	public Properties getProperties();

	/**
	 * Called when exporter should start generating its output
	 */
	public void start();

}
