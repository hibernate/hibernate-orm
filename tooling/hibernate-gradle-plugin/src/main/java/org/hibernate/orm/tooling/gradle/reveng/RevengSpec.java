/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

/**
 * DSL extension for configuring reverse engineering - available as {@code project.hibernate.reveng { ... }}
 */
public class RevengSpec {

	public static final String DEFAULT_HIBERNATE_PROPERTIES = "hibernate.properties";

	public String sqlToRun = "";
	public String hibernateProperties = DEFAULT_HIBERNATE_PROPERTIES;
	public String outputFolder = "generated-sources";
	public String packageName = "";
	public String revengStrategy = null;
	public String revengFile = null;
	public Boolean generateAnnotations = true;
	public Boolean useGenerics = true;
	public String templatePath = null;

}
