/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.query.results;

import org.hibernate.annotations.Imported;

/**
 * @author Steve Ebersole
 */
@Imported
public class Dto2 {
	private final String text;

	public Dto2(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
