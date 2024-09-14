/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.build.annotations.structure;


/**
 * Type implementation Strings
 *
 * @author Steve Ebersole
 */
public class StringType implements Type {
	public static final StringType STRING_TYPE = new StringType();

	public StringType() {
	}

	@Override
	public String getTypeDeclarationString() {
		return "String";
	}
}
