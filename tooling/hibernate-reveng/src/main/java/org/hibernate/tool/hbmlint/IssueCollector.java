package org.hibernate.tool.hbmlint;

public interface IssueCollector {

	public abstract void reportIssue(Issue analyze);

}