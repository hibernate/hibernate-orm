/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.persistence.sql.ColumnMapping;
import jakarta.persistence.sql.ConstructorMapping;
import jakarta.persistence.sql.EntityMapping;
import jakarta.persistence.sql.MappingElement;
import jakarta.persistence.sql.TupleMapping;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.complete.ResultBuilderTuple;

import java.util.function.Consumer;

/// Represents a [TupleMapping] as a [ResultMemento].
///
/// @author Steve Ebersole
public class ResultMementoTuple implements ResultMemento {
	private final TupleMapping tupleMapping;
	private final Element[] elements;

	private ResultMementoTuple(TupleMapping tupleMapping, Element[] elements) {
		this.tupleMapping = tupleMapping;
		this.elements = elements;
	}

	public TupleMapping getTupleMapping() {
		return tupleMapping;
	}

	public Element[] getElements() {
		return elements;
	}

	public static ResultMementoTuple from(TupleMapping tupleMapping, SessionFactoryImplementor factory) {
		final Element[] mementoElements = new Element[ tupleMapping.elements().length ];
		for ( int i = 0; i < tupleMapping.elements().length; i++ ) {
			final MappingElement<?> mappingElement = tupleMapping.elements()[i];
			if ( mappingElement instanceof ColumnMapping<?> columnMapping ) {
				mementoElements[i] = new Element(
						columnMapping,
						ResultMementoBasicStandard.from( columnMapping, factory )
				);
			}
			else if ( mappingElement instanceof ConstructorMapping<?> constructorMapping ) {
				mementoElements[i] = new Element(
						constructorMapping,
						ResultMementoInstantiationStandard.from( constructorMapping, factory )
				);
			}
			else if ( mappingElement instanceof EntityMapping<?> entityMapping ) {
				mementoElements[i] = new Element(
						entityMapping,
						ResultMementoEntityJpa.from( entityMapping, factory )
				);
			}
			else {
				throw new IllegalArgumentException( "Unsupported jakarta.persistence.sql.MappingElement type : " + mappingElement.getClass().getName() );
			}
		}
		return new ResultMementoTuple( tupleMapping, mementoElements );
	}

	@Override
	public Class<?> getResultJavaType() {
		return Tuple.class;
	}

	@Override
	public ResultBuilder resolve(Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context) {
		return ResultBuilderTuple.from(
				this,
				querySpaceConsumer,
				context
		);
	}

	@Override
	public TupleMapping toJpaMapping(SessionFactory sessionFactory) {
		return tupleMapping;
	}

	public record Element(TupleElement<?> tupleElement, ResultMemento resultMemento) {
	}
}
