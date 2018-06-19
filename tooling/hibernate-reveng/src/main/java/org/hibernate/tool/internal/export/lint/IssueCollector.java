package org.hibernate.tool.internal.export.lint;

public interface IssueCollector {

	public abstract void reportIssue(Issue analyze);

}