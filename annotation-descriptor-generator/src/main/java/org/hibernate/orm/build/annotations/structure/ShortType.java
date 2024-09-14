/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.build.annotations.structure;


/**
 * @author Steve Ebersole
 */
public class ShortType implements Type {
	public static final ShortType SHORT_TYPE = new ShortType();

	@Override
	public String getTypeDeclarationString() {
		return "short";
	}
}
