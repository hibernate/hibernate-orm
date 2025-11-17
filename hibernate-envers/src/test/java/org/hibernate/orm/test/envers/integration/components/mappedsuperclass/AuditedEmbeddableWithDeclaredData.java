/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.mappedsuperclass;

import jakarta.persistence.Embeddable;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Embeddable
@Audited
public class AuditedEmbeddableWithDeclaredData extends AbstractAuditedEmbeddable {

	private String codeArt;

	public AuditedEmbeddableWithDeclaredData(int code, String codeArt) {
		super( code );
		this.codeArt = codeArt;
	}

	// Needed for @Embeddable
	protected AuditedEmbeddableWithDeclaredData() {
		this( UNDEFINED, null );
	}

	public String getCodeart() {
		return codeArt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ( ( codeArt == null ) ? 0 : codeArt.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals( obj ) ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		AuditedEmbeddableWithDeclaredData other = (AuditedEmbeddableWithDeclaredData) obj;
		if ( codeArt == null ) {
			if ( other.codeArt != null ) {
				return false;
			}
		}
		else if ( !codeArt.equals( other.codeArt ) ) {
			return false;
		}
		return true;
	}

}
