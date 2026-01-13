/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.sql.ColumnMapping;
import jakarta.persistence.sql.ConstructorMapping;
import jakarta.persistence.sql.MappingElement;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.named.ResultMemento;
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

	public static <T> ResultMementoInstantiationStandard from(
			ConstructorMapping<T> constructorMapping,
			SessionFactoryImplementor factory) {
		final JavaType<T> targetType = factory.getTypeConfiguration()
				.getJavaTypeRegistry()
				.resolveDescriptor( constructorMapping.targetClass() );
		final List<ArgumentMemento> args = CollectionHelper.arrayList( constructorMapping.arguments().length );
		for ( int i = 0; i < constructorMapping.arguments().length; i++ ) {
			final MappingElement<?> argument = constructorMapping.arguments()[i];
			final ResultMemento conversion;
			if ( argument instanceof ColumnMapping<?> columnMapping ) {
				conversion = ResultMementoBasicStandard.from( columnMapping, factory );
			}
			else if ( argument instanceof ConstructorMapping<?> constructorMapping1 ) {
				conversion = from( constructorMapping1, factory );
			}
			else {
				throw new IllegalArgumentException( "Unsupported jakarta.persistence.sql.MappingElement type : " + argument.getClass().getName() );
			}
			args.add( new ArgumentMemento( conversion ) );
		}
		return new ResultMementoInstantiationStandard( targetType, args );
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
