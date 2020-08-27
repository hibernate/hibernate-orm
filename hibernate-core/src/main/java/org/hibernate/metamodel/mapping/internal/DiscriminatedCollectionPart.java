/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.mapping.Any;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.type.AnyType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DiscriminatedCollectionPart implements DiscriminatedAssociationModelPart, CollectionPart {
	private final Nature nature;

	private final NavigableRole partRole;
	private final DiscriminatedAssociationMapping discriminatorMapping;

	public DiscriminatedCollectionPart(
			Nature nature,
			NavigableRole collectionRole,
			JavaTypeDescriptor<Object> baseAssociationJtd,
			Any bootValueMapping,
			AnyType anyType,
			MappingModelCreationProcess creationProcess) {
		this.nature = nature;
		this.partRole = collectionRole.append( nature.getName() );

		this.discriminatorMapping = DiscriminatedAssociationMapping.from(
				partRole,
				baseAssociationJtd,
				this,
				anyType,
				bootValueMapping,
				creationProcess
		);
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public BasicValuedModelPart getDiscriminatorPart() {
		return discriminatorMapping.getDiscriminatorPart();
	}

	@Override
	public BasicValuedModelPart getKeyPart() {
		return discriminatorMapping.getKeyPart();
	}

	@Override
	public EntityMappingType resolveDiscriminatorValue(Object discriminatorValue) {
		return discriminatorMapping.resolveDiscriminatorValueToEntityName( discriminatorValue );
	}

	@Override
	public String getFetchableName() {
		return nature.getName();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return discriminatorMapping;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		return discriminatorMapping.generateFetch(
				fetchParent,
				fetchablePath,
				fetchTiming,
				selected,
				lockMode,
				resultVariable,
				creationState
		);
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return discriminatorMapping.createDomainResult(
				navigablePath,
				tableGroup,
				resultVariable,
				creationState
		);
	}

	@Override
	public MappingType getPartMappingType() {
		return discriminatorMapping;
	}

	@Override
	public JavaTypeDescriptor<?> getJavaTypeDescriptor() {
		return discriminatorMapping.getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return partRole;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return discriminatorMapping.getModelPart().findContainingEntityMapping();
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		if ( AnyDiscriminatorPart.ROLE_NAME.equals( name ) ) {
			return getDiscriminatorPart();
		}

		if ( AnyKeyPart.ROLE_NAME.equals( name ) ) {
			return getKeyPart();
		}

		return null;
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		consumer.accept( getDiscriminatorPart() );
		consumer.accept( getKeyPart() );
	}

	@Override
	public int getNumberOfFetchables() {
		return 2;
	}
}
