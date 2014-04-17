/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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