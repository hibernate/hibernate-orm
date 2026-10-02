package org.hibernate.tool.reveng.internal.export.lint;

public interface IssueCollector {

	public abstract void reportIssue(Issue analyze);

}
