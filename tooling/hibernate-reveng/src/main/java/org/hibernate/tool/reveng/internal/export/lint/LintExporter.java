/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.lint;

import org.hibernate.tool.reveng.internal.export.common.GenericExporter;

public class LintExporter extends GenericExporter {

	private static final String TEXT_REPORT_FTL = "lint/text-report.ftl";

	public void start() {
		getProperties().put(TEMPLATE_NAME, TEXT_REPORT_FTL);
		getProperties().put(FILE_PATTERN, "lint-result.txt");
		super.start();
	}
	protected void setupContext() {
		Lint lint = Lint.createInstance();
		lint.analyze( getMetadata() );
		getProperties().put("lintissues", lint.getResults());
		super.setupContext();
	}

	public String getName() {
		return "lint";
	}


}
