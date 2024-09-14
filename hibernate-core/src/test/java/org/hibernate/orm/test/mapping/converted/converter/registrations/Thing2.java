/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations;

import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public class Thing2 implements Thing, Serializable {
	private final String content;

	public Thing2(String content) {
		this.content = content;
	}

	@Override
	public String getContent() {
		return content;
	}
}
