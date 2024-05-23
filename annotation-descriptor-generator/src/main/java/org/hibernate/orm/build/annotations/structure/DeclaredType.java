/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.build.annotations.structure;

/**
 * @author Steve Ebersole
 */
public class DeclaredType implements Type {
	private final javax.lang.model.type.DeclaredType underlyingType;

	public DeclaredType(javax.lang.model.type.DeclaredType underlyingType) {
		this.underlyingType = underlyingType;
	}

	@Override
	public String getTypeDeclarationString() {
		return underlyingType.toString();
	}
}
