/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.LinkedList;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Represents the "type" of an any-valued mapping
 *
 * @author Steve Ebersole
 */
public class DiscriminatedAssociationMapping implements MappingType, FetchOptions {
	public static final String KEY_ROLE_NAME = "{key}";

	private final DiscriminatedAssociationModelPart modelPart;
	private final AnyDiscriminatorPart discriminatorPart;
	private final Fetchable keyPart;

	private final JavaTypeDescriptor<?> baseAssociationJtd;

	private final FetchTiming fetchTiming;

	private static class ValueMapping {
		private final Object discriminatorValue;
		private final EntityMappingType entityMapping;

		public ValueMapping(Object discriminatorValue, EntityMappingType entityMapping) {
			this.discriminatorValue = discriminatorValue;
			this.entityMapping = entityMapping;
		}
	}

	private final LinkedList<ValueMapping> discriminatorValueMappings = new LinkedList<>();

	public DiscriminatedAssociationMapping(
			DiscriminatedAssociationModelPart modelPart,
			AnyDiscriminatorPart discriminatorPart,
			Fetchable keyPart,
			JavaTypeDescriptor<?> baseAssociationJtd,
			FetchTiming fetchTiming,
			SessionFactoryImplementor sessionFactory) {
		this.modelPart = modelPart;
		this.discriminatorPart = discriminatorPart;
		this.keyPart = keyPart;
		this.baseAssociationJtd = baseAssociationJtd;
		this.fetchTiming = fetchTiming;

		final RuntimeMetamodels runtimeMetamodels = sessionFactory.getRuntimeMetamodels();
		discriminatorPart.getMetaType().getDiscriminatorValuesToEntityNameMap().forEach(
				(value, entityName) -> discriminatorValueMappings.add(
						new ValueMapping( value, runtimeMetamodels.getEntityMappingType( entityName ) )
				)
		);
	}

	public DiscriminatedAssociationModelPart getModelPart() {
		return modelPart;
	}

	public BasicValuedModelPart getDiscriminatorPart() {
		return discriminatorPart;
	}

	public Fetchable getKeyPart() {
		return keyPart;
	}

	public EntityMappingType resolveDiscriminatorValueToEntityName(Object discriminatorValue) {
		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < discriminatorValueMappings.size(); i++ ) {
			final ValueMapping valueMapping = discriminatorValueMappings.get( i );
			if ( valueMapping.discriminatorValue.equals( discriminatorValue ) ) {
				return valueMapping.entityMapping;
			}
		}

		return null;
	}

	public MappingType getPartMappingType() {
		return this;
	}

	public JavaTypeDescriptor<?> getJavaTypeDescriptor() {
		return baseAssociationJtd;
	}

	@Override
	public JavaTypeDescriptor<?> getMappedJavaTypeDescriptor() {
		return baseAssociationJtd;
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.SELECT;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}

	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		final Fetch discriminatorValueFetch = getDiscriminatorPart().generateFetch(
				fetchParent,
				fetchablePath.append( AnyDiscriminatorPart.PART_NAME ),
				FetchTiming.IMMEDIATE,
				selected,
				lockMode,
				resultVariable,
				creationState
		);

		final Fetch keyValueFetch = getKeyPart().generateFetch(
				fetchParent,
				fetchablePath.append( ForeignKeyDescriptor.PART_NAME ),
				FetchTiming.IMMEDIATE,
				selected,
				lockMode,
				resultVariable,
				creationState
		);

		return new AnyValuedFetch(
				fetchablePath,
				modelPart,
				discriminatorValueFetch,
				keyValueFetch,
				fetchTiming,
				fetchParent
		);
	}

	private static class AnyValuedFetch implements Fetch {
		private final NavigablePath navigablePath;

		private final DiscriminatedAssociationModelPart fetchedPart;

		private final Fetch discriminatorValueFetch;
		private final Fetch keyValueFetch;
		private final FetchTiming fetchTiming;
		private final FetchParent fetchParent;

		public AnyValuedFetch(
				NavigablePath navigablePath,
				DiscriminatedAssociationModelPart fetchedPart,
				Fetch discriminatorValueFetch,
				Fetch keyValueFetch,
				FetchTiming fetchTiming,
				FetchParent fetchParent) {
			this.navigablePath = navigablePath;
			this.fetchedPart = fetchedPart;
			this.discriminatorValueFetch = discriminatorValueFetch;
			this.keyValueFetch = keyValueFetch;
			this.fetchTiming = fetchTiming;
			this.fetchParent = fetchParent;
		}

		@Override
		public JavaTypeDescriptor<?> getResultJavaTypeDescriptor() {
			return fetchedPart.getJavaTypeDescriptor();
		}

		@Override
		public NavigablePath getNavigablePath() {
			return navigablePath;
		}

		@Override
		public FetchParent getFetchParent() {
			return fetchParent;
		}

		@Override
		public DiscriminatedAssociationModelPart getFetchedMapping() {
			return fetchedPart;
		}

		@Override
		public FetchTiming getTiming() {
			return fetchTiming;
		}

		@Override
		public boolean hasTableGroup() {
			return false;
		}

		@Override
		public DomainResult<?> asResult(DomainResultCreationState creationState) {
			throw new UnsupportedOperationException();
		}

		@Override
		public DomainResultAssembler<?> createAssembler(
				FetchParentAccess parentAccess,
				AssemblerCreationState creationState) {
			return new AnyResultAssembler(
					navigablePath,
					fetchedPart,
					fetchTiming,
					discriminatorValueFetch.createAssembler( parentAccess, creationState ),
					keyValueFetch.createAssembler( parentAccess, creationState )
			);
		}
	}

	private static class AnyResultAssembler implements DomainResultAssembler<Object> {
		private final NavigablePath fetchedPath;

		private final DiscriminatedAssociationModelPart fetchedPart;
		private final FetchTiming fetchTiming;

		private final DomainResultAssembler<?> discriminatorValueAssembler;
		private final DomainResultAssembler<?> keyValueAssembler;

		public AnyResultAssembler(
				NavigablePath fetchedPath,
				DiscriminatedAssociationModelPart fetchedPart,
				FetchTiming fetchTiming,
				DomainResultAssembler<?> discriminatorValueAssembler,
				DomainResultAssembler<?> keyValueAssembler) {
			this.fetchedPath = fetchedPath;
			this.fetchedPart = fetchedPart;
			this.fetchTiming = fetchTiming;
			this.discriminatorValueAssembler = discriminatorValueAssembler;
			this.keyValueAssembler = keyValueAssembler;
		}

		@Override
		public Object assemble(
				RowProcessingState rowProcessingState,
				JdbcValuesSourceProcessingOptions options) {
			final Object discriminatorValue = discriminatorValueAssembler.assemble( rowProcessingState, options );

			if ( discriminatorValue == null ) {
				// null association
				assert keyValueAssembler.assemble( rowProcessingState, options ) == null;
				return null;
			}

			final EntityMappingType entityMapping = fetchedPart.resolveDiscriminatorValue( discriminatorValue );

			final Object keyValue = keyValueAssembler.assemble( rowProcessingState, options );

			final SharedSessionContractImplementor session = rowProcessingState
					.getJdbcValuesSourceProcessingState()
					.getSession();

			return session.internalLoad(
					entityMapping.getEntityName(),
					keyValue,
					fetchTiming != FetchTiming.DELAYED,
					// should not be null since we checked already.  null would indicate bad data (ala, not-found handling)
					false
			);
		}

		@Override
		public JavaTypeDescriptor<Object> getAssembledJavaTypeDescriptor() {
			//noinspection unchecked
			return fetchedPart.getJavaTypeDescriptor();
		}

		@Override
		public String toString() {
			return "AnyResultAssembler( `" + fetchedPath + "` )";
		}
	}
}
