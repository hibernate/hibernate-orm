/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.Loader;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/**
 * @author Andrea Boriero
 */
public class CollectionElementLoaderByIndex implements Loader {
	private final PluralAttributeMapping attributeMapping;
	private final SelectStatement sqlAst;
	private final JdbcParametersList jdbcParameters;
	private final int baseIndex;

	private final int keyJdbcCount;

	/**
	 * Shortened form of {@link #CollectionElementLoaderByIndex(PluralAttributeMapping, int, LoadQueryInfluencers, SessionFactoryImplementor)}
	 * which applied the collection mapping's {@linkplain PluralAttributeMapping.IndexMetadata#getListIndexBase()}
	 */
	public CollectionElementLoaderByIndex(
			PluralAttributeMapping attributeMapping,
			LoadQueryInfluencers influencers,
			SessionFactoryImplementor sessionFactory) {
		this( attributeMapping, attributeMapping.getIndexMetadata().getListIndexBase(), influencers, sessionFactory );
	}

	/**
	 * @param baseIndex A base value to apply to the relational index values processed on {@link #incrementIndexByBase}
	 */
	public CollectionElementLoaderByIndex(
			PluralAttributeMapping attributeMapping,
			int baseIndex,
			LoadQueryInfluencers influencers,
			SessionFactoryImplementor sessionFactory) {
		this.attributeMapping = attributeMapping;
		this.baseIndex = baseIndex;

		final var keyDescriptor = attributeMapping.getKeyDescriptor();
		final var indexDescriptor = attributeMapping.getIndexDescriptor();

		final List<ModelPart> restrictedParts = new ArrayList<>();
		restrictedParts.add( keyDescriptor );

		if ( indexDescriptor instanceof EntityCollectionPart entityCollectionPart ) {
			var identifierMapping =
					entityCollectionPart.getEntityMappingType()
							.getIdentifierMapping();
			restrictedParts.add( identifierMapping );
			keyJdbcCount = keyDescriptor.getJdbcTypeCount() + identifierMapping.getJdbcTypeCount();
		}
		else {
			restrictedParts.add( indexDescriptor );
			keyJdbcCount = keyDescriptor.getJdbcTypeCount() + indexDescriptor.getJdbcTypeCount();
		}

		final List<ModelPart> partsToSelect = new ArrayList<>();
		partsToSelect.add( attributeMapping.getElementDescriptor() );

		final var jdbcParametersBuilder = JdbcParametersList.newBuilder( keyJdbcCount );
		sqlAst = LoaderSelectBuilder.createSelect(
				attributeMapping,
				partsToSelect,
				restrictedParts,
				null,
				1,
				influencers,
				new LockOptions(),
				jdbcParametersBuilder::add,
				sessionFactory
		);
		jdbcParameters = jdbcParametersBuilder.build();
	}

	@Override
	public PluralAttributeMapping getLoadable() {
		return getAttributeMapping();
	}

	public PluralAttributeMapping getAttributeMapping() {
		return attributeMapping;
	}

	public SelectStatement getSqlAst() {
		return sqlAst;
	}

	public JdbcParametersList getJdbcParameters() {
		return jdbcParameters;
	}

	public Object load(Object key, Object index, SharedSessionContractImplementor session) {

		final var jdbcParameterBindings = new JdbcParameterBindingsImpl( keyJdbcCount );

		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				key,
				attributeMapping.getKeyDescriptor(),
				jdbcParameters,
				session
		);
		offset += jdbcParameterBindings.registerParametersForEachJdbcValue(
				incrementIndexByBase( index ),
				offset,
				attributeMapping.getIndexDescriptor(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();

		final var sessionFactory = session.getFactory();
		final var jdbcServices = sessionFactory.getJdbcServices();
		final var jdbcSelect =
				jdbcServices.getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildSelectTranslator( sessionFactory, sqlAst )
						.translate( jdbcParameterBindings, QueryOptions.NONE );
		var list = jdbcServices.getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new BaseExecutionContext( session ),
				RowTransformerStandardImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.FILTER,
				1
		);
		return list.isEmpty() ? null : list.get( 0 );
	}

	/**
	 * If the index being loaded by for a List and the mapping specified a
	 * {@linkplain org.hibernate.annotations.ListIndexBase base-index}, this will return
	 * the passed {@code index} value incremented by the base.  Otherwise, the passed {@code index}
	 * is returned.
	 *
	 * @param index The relational index value; specifically without any mapped base applied
	 *
	 * @return The appropriately incremented base
	 */
	protected Object incrementIndexByBase(Object index) {
		if ( baseIndex > 0 ) {
			return (Integer) index + baseIndex;
		}
		return index;
	}
}
