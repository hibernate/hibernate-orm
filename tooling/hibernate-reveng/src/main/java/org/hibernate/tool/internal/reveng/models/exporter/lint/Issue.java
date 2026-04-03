/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.exporter.lint;

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
