/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.lint;

/**
 * Represents a lint issue found during analysis.
 *
 * @author Koen Aers
 */
public class Issue {

	public static final int HIGH_PRIORITY = 100;
	public static final int NORMAL_PRIORITY = 50;
	public static final int LOW_PRIORITY = 0;

	private final String type;
	private final int priority;
	private final String description;

	public Issue(String type, int priority, String description) {
		this.type = type;
		this.priority = priority;
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public int getPriority() {
		return priority;
	}

	public String getDescription() {
		return description;
	}

	public String toString() {
		return type + ":" + description;
	}
}
