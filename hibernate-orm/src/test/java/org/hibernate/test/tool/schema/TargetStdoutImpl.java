/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.tool.schema;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;

/**
 * @author Steve Ebersole
 */
public class TargetStdoutImpl implements GenerationTarget {
	final private String delimiter;
	final private Formatter formatter;

	public TargetStdoutImpl() {
		this( null );
	}
	
	public TargetStdoutImpl(String delimiter) {
		this( delimiter, FormatStyle.NONE.getFormatter());
	}

	public TargetStdoutImpl(String delimiter, Formatter formatter) {
		this.formatter = formatter;
		this.delimiter = delimiter;
	}

	@Override
	public void prepare() {
	}

	@Override
	public void accept(String action) {
		if (formatter != null) {
			action = formatter.format(action);
		}
		if ( delimiter != null ) {
			action += delimiter;
		}
		System.out.println( action );
	}

	@Override
	public void release() {
	}

}
