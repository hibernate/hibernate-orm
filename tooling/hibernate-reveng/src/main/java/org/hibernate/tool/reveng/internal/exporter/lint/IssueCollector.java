/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.lint;

/**
 * Collects {@link Issue}s reported by detectors.
 *
 * @author Koen Aers
 */
public interface IssueCollector {

	void reportIssue(Issue issue);
}
