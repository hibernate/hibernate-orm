/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.MutationGroup;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.internal.MutationGroupNone;
import org.hibernate.sql.model.internal.MutationGroupSingle;
import org.hibernate.sql.model.internal.MutationGroupStandard;

/**
 * Builder (pattern) for {@link TableMutation} references
 *
 * @author Steve Ebersole
 */
public class MutationGroupBuilder implements SelectableConsumer {
	private final MutationType mutationType;
	private final EntityMutationTarget mutationTarget;

	private final Map<String, TableMutationBuilder<?>> tableMutationBuilderMap;

	public MutationGroupBuilder(MutationType mutationType, EntityMutationTarget mutationTarget) {
		this.mutationType = mutationType;
		this.mutationTarget = mutationTarget;
		this.tableMutationBuilderMap = new LinkedHashMap<>();
	}

	public MutationType getMutationType() {
		return mutationType;
	}

	public EntityMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	public <B extends TableMutationBuilder<?>> B findTableDetailsBuilder(String name) {
		//noinspection unchecked
		return (B) tableMutationBuilderMap.get( name );
	}

	public <B extends TableMutationBuilder<?>> B getTableDetailsBuilder(String name) {
		final B builder = findTableDetailsBuilder( name );
		if ( builder == null ) {
			throw new RuntimeException(
					"Expecting already existing TableMutationBuilder : " + name
			);
		}
		return builder;
	}

	public void addTableDetailsBuilder(TableMutationBuilder<?> builder) {
		tableMutationBuilderMap.put( builder.getMutatingTable().getTableName(), builder );
	}

	public void forEachTableMutationBuilder(Consumer<TableMutationBuilder<?>> consumer) {
		tableMutationBuilderMap.forEach( (name, mutationBuilder) -> consumer.accept( mutationBuilder ) );
	}

	@Override
	public void accept(int selectionIndex, SelectableMapping selectableMapping) {
		final EntityPersister entityPersister = mutationTarget.getTargetPart().getEntityPersister();
		final String tableNameForMutation = entityPersister.physicalTableNameForMutation( selectableMapping );
		final ColumnValuesTableMutationBuilder mutationBuilder = findTableDetailsBuilder( tableNameForMutation );
		mutationBuilder.addValueColumn( selectableMapping );
	}

	public MutationGroup buildMutationGroup() {
		if ( tableMutationBuilderMap.isEmpty() ) {
			throw new IllegalStateException(
					String.format(
							Locale.ROOT,
							"Mutation group contained no table mutations - %s : `%s`",
							mutationType,
							mutationTarget.getNavigableRole().getFullPath()
					)
			);
		}

		if ( tableMutationBuilderMap.size() == 1 ) {
			final TableMutationBuilder<?> tableMutationBuilder = tableMutationBuilderMap.entrySet().iterator().next().getValue();
			final TableMutation<?> mutation = tableMutationBuilder.buildMutation();
			if ( mutation == null ) {
				return new MutationGroupNone( mutationType, mutationTarget );
			}
			return new MutationGroupSingle( mutationType, mutationTarget, mutation );
		}

		final ArrayList<TableMutation<?>> tableMutations = new ArrayList<>( tableMutationBuilderMap.size() );
		tableMutationBuilderMap.forEach( (name, tableDetailsBuilder) -> {
			final TableMutation<?> tableMutation = tableDetailsBuilder.buildMutation();
			if ( tableMutation != null ) {
				tableMutations.add( tableMutation );
			}
		} );

		if ( tableMutations.isEmpty() ) {
			return new MutationGroupNone( mutationType, mutationTarget );
		}

		if ( tableMutations.size() == 1 ) {
			return new MutationGroupSingle( mutationType, mutationTarget, tableMutations.get( 0 ) );
		}

		return new MutationGroupStandard( mutationType, mutationTarget, tableMutations );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"MutationGroupBuilder( %s:`%s` )",
				mutationType.name(),
				mutationTarget.getNavigableRole().getFullPath()
		);
	}
}
