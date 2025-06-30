/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.transform.ResultTransformer;

public class AliasInjectingTransformer extends BasicTransformerAdapter {

	private final String[] aliases;
	private final ResultTransformer resultTransformer;

	public AliasInjectingTransformer(String[] aliases, ResultTransformer resultTransformer) {
		this.aliases = aliases;
		this.resultTransformer = resultTransformer;
	}

	public String[] getAliases() {
		return aliases;
	}

	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		return resultTransformer.transformTuple(tuple, this.aliases);
	}
}
