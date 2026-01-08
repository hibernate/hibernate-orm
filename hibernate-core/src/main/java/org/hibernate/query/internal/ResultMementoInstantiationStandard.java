/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.sql.ConstructorMapping;
import jakarta.persistence.sql.MappingElement;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.SessionFactory;
import org.hibernate.query.named.ResultMementoInstantiation;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderInstantiation;
import org.hibernate.type.descriptor.java.JavaType;

import static java.util.Collections.unmodifiableList;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
public class ResultMementoInstantiationStandard implements ResultMementoInstantiation {

	private final JavaType<?> instantiatedJtd;
	private final List<ArgumentMemento> argumentMementos;

	public ResultMementoInstantiationStandard(
			JavaType<?> instantiatedJtd,
			List<ArgumentMemento> argumentMementos) {
		this.instantiatedJtd = instantiatedJtd;
		this.argumentMementos = argumentMementos;
	}

	public JavaType<?> getInstantiatedJavaType() {
		return instantiatedJtd;
	}

	public List<ArgumentMemento> getArgumentMementos() {
		return unmodifiableList( argumentMementos );
	}

	@Override
	public Class<?> getResultJavaType() {
		return instantiatedJtd.getJavaTypeClass();
	}

	@Override
	public ResultBuilder resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final List<ResultBuilder> argumentBuilders = arrayList( argumentMementos.size() );
		argumentMementos.forEach(
				argumentMemento -> argumentBuilders.add(
						argumentMemento.resolve( querySpaceConsumer, context )
				)
		);
		return new CompleteResultBuilderInstantiation( instantiatedJtd, argumentBuilders );
	}

	@Override
	public <R> ResultSetMapping<R> toJpaMapping(SessionFactory sessionFactory) {
		//noinspection unchecked
		return new ConstructorMapping<>(
				(Class<R>) instantiatedJtd.getJavaTypeClass(),
				convertArguments( sessionFactory )
		);
	}

	private MappingElement<?>[] convertArguments(SessionFactory sessionFactory) {
		var arguments = new MappingElement<?>[ argumentMementos.size() ];
		for ( int i = 0; i < arguments.length; i++ ) {
			var argumentMemento = argumentMementos.get( i );
			arguments[i] = argumentMemento.toJpaMapping( sessionFactory );
		}
		return arguments;
	}
}
