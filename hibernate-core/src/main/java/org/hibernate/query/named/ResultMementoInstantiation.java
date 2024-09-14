/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.named;

import java.util.function.Consumer;

import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.ResultBuilder;

/**
 * @author Steve Ebersole
 */
public interface ResultMementoInstantiation extends ResultMemento {
	class ArgumentMemento {
		private final ResultMemento argumentMemento;

		public ArgumentMemento(ResultMemento argumentMemento) {
			this.argumentMemento = argumentMemento;
		}

		public ResultBuilder resolve(Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context) {
			return argumentMemento.resolve( querySpaceConsumer, context );
		}
	}
}
