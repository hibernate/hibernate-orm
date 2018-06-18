package org.hibernate.tool.internal.export.lint;

import org.hibernate.tool.hbmlint.Issue;

public interface IssueCollector {

	public abstract void reportIssue(Issue analyze);

}