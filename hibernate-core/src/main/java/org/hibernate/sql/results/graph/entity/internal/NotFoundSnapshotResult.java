/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class NotFoundSnapshotResult implements DomainResult {
	private final NavigablePath navigablePath;
	private final ToOneAttributeMapping toOneMapping;

	private final DomainResult<?> keyResult;
	private final DomainResult<?> targetResult;

	public NotFoundSnapshotResult(
			NavigablePath navigablePath,
			ToOneAttributeMapping toOneMapping,
			TableGroup keyTableGroup,
			TableGroup targetTableGroup,
			DomainResultCreationState creationState) {
		this.navigablePath = navigablePath;
		this.toOneMapping = toOneMapping;

		// NOTE: this currently assumes that only the key side can be
		// defined with `@NotFound`.  That feels like a reasonable
		// assumption, though there is sme question whether to support
		// this for the inverse side also when a join table is used.
		//
		// however, that would mean a 1-1 with a join-table which
		// is pretty odd mapping
		final ForeignKeyDescriptor fkDescriptor = toOneMapping.getForeignKeyDescriptor();
		this.keyResult = fkDescriptor.createKeyDomainResult( navigablePath, keyTableGroup, creationState );
		this.targetResult = fkDescriptor.createTargetDomainResult( navigablePath, targetTableGroup, creationState );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return toOneMapping.getJavaType();
	}

	@Override
	public String getResultVariable() {
		return null;
	}

	@Override
	public DomainResultAssembler<Object> createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		return new NotFoundSnapshotAssembler(
				navigablePath,
				toOneMapping,
				keyResult.createResultAssembler( parentAccess, creationState ),
				targetResult.createResultAssembler( parentAccess, creationState )
		);
	}

}
