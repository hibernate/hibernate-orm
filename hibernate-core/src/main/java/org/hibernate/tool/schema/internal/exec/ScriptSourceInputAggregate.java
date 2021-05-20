/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;
import org.hibernate.tool.schema.spi.ScriptSourceInput;

/**
 * A script source input that aggregates over multiple other {@link ScriptSourceInput}.
 *
 * @author Christian Beikov
 */
public class ScriptSourceInputAggregate implements ScriptSourceInput {

	private final ScriptSourceInput[] inputs;

	/**
	 * Constructs a ScriptSourceInputAggregate
	 *
	 * @param inputs The script source inputs
	 */
	public ScriptSourceInputAggregate(ScriptSourceInput[] inputs) {
		this.inputs = inputs;
	}

	@Override
	public void prepare() {
		for ( ScriptSourceInput input : inputs ) {
			input.prepare();
		}
	}

	@Override
	public void release() {
		Throwable t = null;
		for ( ScriptSourceInput input : inputs ) {
			try {
				input.release();
			}
			catch (Throwable t2) {
				if ( t == null ) {
					t = t2;
				}
				else {
					t.addSuppressed( t2 );
				}
			}
		}
		if ( t != null ) {
			doThrow( t );
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void doThrow(Throwable e) throws T {
		throw (T) e;
	}

	@Override
	public List<String> read(ImportSqlCommandExtractor commandExtractor) {
		final List<String>[] lists = new List[inputs.length];
		int size = 0;
		for ( int i = 0; i < inputs.length; i++ ) {
			lists[i] = inputs[i].read( commandExtractor );
			size += lists[i].size();
		}

		final List<String> list = new ArrayList<>( size );
		for ( List<String> strings : lists ) {
			list.addAll( strings );
		}
		return list;
	}

	@Override
	public String toString() {
		return "ScriptSourceInputAggregate(" + Arrays.toString( inputs ) + ")";
	}
}
