/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.docs;

import java.util.List;
import java.util.Map;

import org.hibernate.orm.ReleaseFamilyIdentifier;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

/**
 * Binding for the doc-pub descriptor (JSON) file
 *
 * @author Steve Ebersole
 */
@JsonbPropertyOrder( {"name", "stableFamily", "singlePageDetails", "multiPageDetails", "releaseFamilies" } )
public class ProjectDocumentationDescriptor {
	@JsonbProperty( "project" )
	private String name;

	@JsonbProperty( "stable" )
	@JsonbTypeAdapter( ReleaseFamilyIdentifierMarshalling.class )
	private ReleaseFamilyIdentifier stableFamily;

	@JsonbProperty( "versions" )
	private List<ReleaseFamilyDocumentation> releaseFamilies;

	@JsonbProperty( "multi" )
	private Map<String,String> multiPageDetails;
	@JsonbProperty( "single" )
	private Map<String,String> singlePageDetails;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ReleaseFamilyIdentifier getStableFamily() {
		return stableFamily;
	}

	public void setStableFamily(ReleaseFamilyIdentifier stableFamily) {
		this.stableFamily = stableFamily;
	}

	public List<ReleaseFamilyDocumentation> getReleaseFamilies() {
		return releaseFamilies;
	}

	public void setReleaseFamilies(List<ReleaseFamilyDocumentation> releaseFamilies) {
		this.releaseFamilies = releaseFamilies;
	}

	public void addReleaseFamily(ReleaseFamilyDocumentation familyDetails) {
		// Add new entries at the top
		releaseFamilies.add( 0, familyDetails );
	}

	public Map<String, String> getMultiPageDetails() {
		return multiPageDetails;
	}

	public void setMultiPageDetails(Map<String, String> multiPageDetails) {
		this.multiPageDetails = multiPageDetails;
	}

	public Map<String, String> getSinglePageDetails() {
		return singlePageDetails;
	}

	public void setSinglePageDetails(Map<String, String> singlePageDetails) {
		this.singlePageDetails = singlePageDetails;
	}
}
