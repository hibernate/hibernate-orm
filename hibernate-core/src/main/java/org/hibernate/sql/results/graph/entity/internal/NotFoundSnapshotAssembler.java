/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.FetchNotFoundException;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Specialized DomainResultAssembler for {@link org.hibernate.annotations.NotFound} associations
 *
 * @author Steve Ebersole
 */
public class NotFoundSnapshotAssembler implements DomainResultAssembler {
	private final NavigablePath navigablePath;
	private final ToOneAttributeMapping toOneMapping;
	private final DomainResultAssembler<?> keyValueAssembler;
	private final DomainResultAssembler<?> targetValueAssembler;

	public NotFoundSnapshotAssembler(
			NavigablePath navigablePath,
			ToOneAttributeMapping toOneMapping,
			DomainResultAssembler<?> keyValueAssembler,
			DomainResultAssembler<?> targetValueAssembler) {
		assert toOneMapping.hasNotFoundAction();

		this.navigablePath = navigablePath;
		this.toOneMapping = toOneMapping;
		this.keyValueAssembler = keyValueAssembler;
		this.targetValueAssembler = targetValueAssembler;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		final Object keyValue = keyValueAssembler.assemble( rowProcessingState );
		final Object targetValue = targetValueAssembler.assemble( rowProcessingState );

		// because of `@NotFound` these could be mismatched
		if ( keyValue != null ) {
			if ( targetValue != null ) {
				if ( toOneMapping.getNotFoundAction() == NotFoundAction.IGNORE ) {
					return null;
				}
				else {
					throw new FetchNotFoundException(
							toOneMapping.getAssociatedEntityMappingType().getEntityName(),
							keyValue
					);
				}
			}
		}

		return targetValue;
	}

	@Override
	public JavaType<?> getAssembledJavaType() {
		return toOneMapping.getJavaType();
	}
}
