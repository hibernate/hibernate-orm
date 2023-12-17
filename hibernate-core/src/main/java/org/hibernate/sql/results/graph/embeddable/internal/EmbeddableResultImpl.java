/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.query.SemanticException;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResult;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EmbeddableResultImpl<T> extends AbstractFetchParent implements EmbeddableResultGraphNode, DomainResult<T>, EmbeddableResult<T> {
	private final String resultVariable;
	private final boolean containsAnyNonScalars;
	private final NavigablePath initializerNavigablePath;
	private final EmbeddableMappingType fetchContainer;

	public EmbeddableResultImpl(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart modelPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		super( navigablePath );

		this.fetchContainer = modelPart.getEmbeddableTypeDescriptor();
		this.resultVariable = resultVariable;

		// We currently don't support explicitly selecting embeddables that contain collections
		// as we wouldn't be able to correctly initialize their owning entity instances
		checkContainsCollections( modelPart, navigablePath );

		/*
			An `{embeddable_result}` sub-path is created for the corresponding initializer to differentiate it from a fetch-initializer if this embedded is also fetched.
			The Jakarta Persistence spec says that any embedded value selected in the result should not be part of the state of any managed entity.
			Using this `{embeddable_result}` sub-path avoids this situation.
		*/
		this.initializerNavigablePath = navigablePath.append( "{embeddable_result}" );

		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();

		fromClauseAccess.resolveTableGroup(
				navigablePath,
				np -> {
					final EmbeddableValuedModelPart embeddedValueMapping = modelPart.getEmbeddableTypeDescriptor().getEmbeddedValueMapping();
					final TableGroup tableGroup = fromClauseAccess.findTableGroup( navigablePath.getParent() );
					final TableGroupJoin tableGroupJoin = embeddedValueMapping.createTableGroupJoin(
							navigablePath,
							tableGroup,
							resultVariable,
							null,
							SqlAstJoinType.INNER,
							true,
							false,
							creationState.getSqlAstCreationState()
					);
					tableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);

		afterInitialize( this, creationState );

		// after-after-initialize :D
		containsAnyNonScalars = determineIfContainedAnyScalars( getFetches() );
	}

	private static boolean determineIfContainedAnyScalars(ImmutableFetchList fetches) {
		for ( Fetch fetch : fetches ) {
			if ( fetch.containsAnyNonScalarResults() ) {
				return true;
			}
		}

		return false;
	}

	private static void checkContainsCollections(
			EmbeddableValuedModelPart embeddableModelPart,
			NavigablePath navigablePath) {
		embeddableModelPart.forEachSubPart( (index, modelPart) -> {
			final AttributeMapping attribute = modelPart.asAttributeMapping();
			if ( attribute != null ) {
				if ( attribute.isPluralAttributeMapping() ) {
					throw new SemanticException( String.format(
							"Explicit selection of an embeddable containing collections is not supported: %s",
							navigablePath
					) );
				}
				else if ( attribute.isEmbeddedAttributeMapping() ) {
					checkContainsCollections( attribute.asEmbeddedAttributeMapping(), navigablePath );
				}
			}
		} );
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return containsAnyNonScalars;
	}

	@Override
	public EmbeddableMappingType getFetchContainer() {
		return this.fetchContainer;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getReferencedMappingType().getJavaType();
	}

	@Override
	public EmbeddableMappingType getReferencedMappingType() {
		return getFetchContainer();
	}

	@Override
	public EmbeddableValuedModelPart getReferencedMappingContainer() {
		return getFetchContainer().getEmbeddedValueMapping();
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final EmbeddableInitializer initializer = creationState.resolveInitializer(
				initializerNavigablePath,
				getReferencedModePart(),
				() -> new EmbeddableResultInitializer(
						this,
						parentAccess,
						creationState
				)
		).asEmbeddableInitializer();

		assert initializer != null;

		//noinspection unchecked
		return new EmbeddableAssembler( initializer );
	}
}
