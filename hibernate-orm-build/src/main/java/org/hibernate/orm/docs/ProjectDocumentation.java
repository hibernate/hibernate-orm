/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.docs;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
public class ProjectDocumentation {
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
