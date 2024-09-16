/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;

import org.hibernate.usertype.UserTypeSupport;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-enums-custom-type-example[]
public class GenderType extends UserTypeSupport<Gender> {
	public GenderType() {
		super(Gender.class, Types.CHAR);
	}
}
//end::basic-enums-custom-type-example[]
