/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.export.lint;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.Metadata;

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
