/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.xjc;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class SchemaDescriptor implements Named {
	private final String name;
	private final Project project;

	private final RegularFileProperty xsdFile;
	private final RegularFileProperty xjcBindingFile;
	private final SetProperty<String> xjcExtensions;

	public SchemaDescriptor(String name, Project project) {
		this.name = name;
		this.project = project;

		xsdFile = project.getObjects().fileProperty();
		xjcBindingFile = project.getObjects().fileProperty();
		xjcExtensions = project.getObjects().setProperty( String.class );
	}

	@Override
	public final String getName() {
		return name;
	}

	@InputFile
	public RegularFileProperty getXsdFile() {
		return xsdFile;
	}

	public void setXsdFile(Object reference) {
		xsdFile.set( project.file( reference ) );
	}

	public void xsdFile(Object reference) {
		setXsdFile( reference );
	}

	@InputFile
	public RegularFileProperty getXjcBindingFile() {
		return xjcBindingFile;
	}

	public void setXjcBindingFile(Object reference) {
		xjcBindingFile.set( project.file( reference ) );
	}

	public void xjcBindingFile(Object reference) {
		setXjcBindingFile( reference );
	}

	@Input
	public SetProperty<String> ___xjcExtensions() {
		return xjcExtensions;
	}

	public Set<String> getXjcExtensions() {
		return xjcExtensions.get();
	}

	public void setXjcExtensions(Set<String> xjcExtensions) {
		this.xjcExtensions.set( xjcExtensions );
	}

	public void setXjcExtensions(String... xjcExtensions) {
		xjcExtensions( xjcExtensions );
	}

	public void xjcExtensions(String... xjcExtensions) {
		setXjcExtensions( new HashSet<>( Arrays.asList( xjcExtensions) ) );
	}
}
