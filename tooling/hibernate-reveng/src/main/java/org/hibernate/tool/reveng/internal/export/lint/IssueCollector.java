/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.lint;

public interface IssueCollector {

	public abstract void reportIssue(Issue analyze);

}
