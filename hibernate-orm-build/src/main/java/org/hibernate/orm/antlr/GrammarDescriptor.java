/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.antlr;

import org.gradle.api.Named;

/**
 * Describes a grammar for generation
 *
 * @author Steve Ebersole
 */
public class GrammarDescriptor implements Named {
	private final String grammarName;
	private final String packageName;

	GrammarDescriptor(String grammarName, String packageName) {
		this.grammarName = grammarName;
		this.packageName = packageName;
	}

	@Override
	public String getName() {
		return getGrammarName();
	}

	public String getPackageName() {
		return packageName;
	}

	public String getGrammarName() {
		return grammarName;
	}
}
