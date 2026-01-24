/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import java.util.function.Consumer;

import jakarta.persistence.sql.MappingElement;
import org.hibernate.SessionFactory;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.spi.ResultBuilder;

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

		public MappingElement<?> toJpaMapping(SessionFactory sessionFactory) {
			return argumentMemento.toJpaMappingElement( sessionFactory );
		}
	}
}
