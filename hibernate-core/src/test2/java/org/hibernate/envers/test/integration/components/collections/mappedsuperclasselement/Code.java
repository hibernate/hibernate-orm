/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.components.collections.mappedsuperclasselement;

import javax.persistence.Embeddable;

/**
 * @author Jakob Braeuchi.
 */
@Embeddable
public class Code extends AbstractCode {

	public Code() {
	}

	public Code(int code) {
		super( code );
	}
}