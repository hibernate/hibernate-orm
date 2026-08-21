/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.lint;

import org.hibernate.boot.Metadata;

import java.util.ArrayList;
import java.util.List;

public class HbmLint implements IssueCollector {


	final Detector[] detectors;

	public HbmLint(Detector[] detectors) {
		this.detectors = detectors;
	}

	List<Issue> results = new ArrayList<Issue>();

	public void analyze(Metadata metadata) {
		for (int i = 0; i < detectors.length; i++) {
			detectors[i].initialize(metadata);
			detectors[i].visit(this);
		}

	}

	/* (non-Javadoc)
	 * @see org.hibernate.tool.hbmlint.IssueCollector#reportProblem(org.hibernate.tool.hbmlint.Issue)
	 */
	public void reportIssue(Issue analyze) {
		results.add(analyze);
	}

	public List<Issue> getResults() {
		return results;
	}

	public static HbmLint createInstance() {
		return new HbmLint(
			new Detector[] {
					new BadCachingDetector(),
					new InstrumentationDetector(),
					new ShadowedIdentifierDetector(),
					new SchemaByMetaDataDetector()
			});

	}

}
