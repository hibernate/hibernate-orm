/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.components.mappedsuperclass;

/**
 * @author Jakob Braeuchi.
 * @author Gail Badner
 */

import javax.persistence.Embeddable;

@Embeddable
public class EmbeddableWithDeclaredData extends AbstractEmbeddable {

	private String codeArt;

	public EmbeddableWithDeclaredData(int code, String codeArt) {
		super( code );
		this.codeArt = codeArt;
	}

	// Needed for @Embeddable
	protected EmbeddableWithDeclaredData() {
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
		EmbeddableWithDeclaredData other = (EmbeddableWithDeclaredData) obj;
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