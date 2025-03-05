/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.docs;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.orm.ReleaseFamilyIdentifier;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

/**
 * Binding for the doc-pub descriptor (JSON) related to a specific release family.
 *
 * @see ProjectDocumentationDescriptor
 *
 * @author Steve Ebersole
 */
@JsonbPropertyOrder( { "name", "redirects" } )
public class ReleaseFamilyDocumentation {
	@JsonbProperty( "version" )
	@JsonbTypeAdapter( ReleaseFamilyIdentifierMarshalling.class )
	private ReleaseFamilyIdentifier name;
	private Map<String,String> redirects;

	public ReleaseFamilyDocumentation() {
	}

	/**
	 * The release family, e.g. `6.0` or `5.6`
	 */
	public ReleaseFamilyIdentifier getName() {
		return name;
	}

	public void setName(ReleaseFamilyIdentifier name) {
		this.name = name;
	}

	public Map<String, String> getRedirects() {
		return redirects;
	}

	public void setRedirects(Map<String, String> redirects) {
		this.redirects = redirects;
	}

	public void redirect(String from, String to) {
		if ( redirects == null ) {
			redirects = new HashMap<>();
		}
		redirects.put( from, to );
	}

	@Override
	public String toString() {
		return "ReleaseFamilyDocumentation( " + name + ")";
	}
}
