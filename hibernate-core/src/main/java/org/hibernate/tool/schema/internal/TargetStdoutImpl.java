/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.tool.schema.spi.Target;

/**
 * @author Steve Ebersole
 */
public class TargetStdoutImpl implements Target {
	@Override
	public boolean acceptsImportScriptActions() {
		return true;
	}

	@Override
	public void prepare() {
	}

	@Override
	public void accept(String action) {
		System.out.println( action );
	}

	@Override
	public void release() {
	}
}
