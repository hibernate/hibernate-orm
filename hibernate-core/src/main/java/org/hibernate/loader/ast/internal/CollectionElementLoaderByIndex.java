package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.Loader;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Andrea Boriero
 */
public class CollectionElementLoaderByIndex implements Loader {
	private final PluralAttributeMapping attributeMapping;
	private final CollectionPartRestriction indexRestriction;
	private final SelectStatement sqlAst;
	private final JdbcSelect jdbcSelect;
	private final JdbcParametersList jdbcParameters;
	private final int baseIndex;

	/**
	 * Shortened form of {@link #CollectionElementLoaderByIndex(PluralAttributeMapping, int, LoadQueryInfluencers, SessionFactoryImplementor)}
	 * which applied the collection mapping's {@linkplain PluralAttributeMapping.IndexMetadata#getListIndexBase()}
	 */
	public CollectionElementLoaderByIndex(
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nonnull LoadQueryInfluencers influencers,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		this( attributeMapping, attributeMapping.getIndexMetadata().getListIndexBase(), influencers, sessionFactory );
	}

	/**
	 * @param baseIndex A base value to apply to the relational index values processed on {@link #incrementIndexByBase}
	 */
	public CollectionElementLoaderByIndex(
			@Nonnull PluralAttributeMapping attributeMapping,
			int baseIndex,
			@Nonnull LoadQueryInfluencers influencers,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		this.attributeMapping = attributeMapping;
		this.baseIndex = baseIndex;

		final var keyDescriptor = attributeMapping.getKeyDescriptor();
		final var indexDescriptor = attributeMapping.getIndexDescriptor();

		final List<ModelPart> restrictedParts = new ArrayList<>();
		restrictedParts.add( keyDescriptor );

		indexRestriction = CollectionPartRestriction.of( attributeMapping, castNonNull( indexDescriptor ) );
		indexRestriction.addRestrictedPart( restrictedParts );
		final int keyJdbcCount = keyDescriptor.getJdbcTypeCount() + indexRestriction.getJdbcTypeCount();

		final List<ModelPart> partsToSelect = List.of( attributeMapping.getElementDescriptor() );

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
				new SqlAliasBaseManager(),
				sessionFactory
		);
		indexRestriction.restrictJoined( sqlAst.getQueryPart().getFirstQuerySpec(), jdbcParametersBuilder::add );
		jdbcParameters = jdbcParametersBuilder.build();
		jdbcSelect = CollectionLookupLoader.translate( sqlAst, sessionFactory );
	}

	@Nonnull
	@Override
	public PluralAttributeMapping getLoadable() {
		return getAttributeMapping();
	}

	@Nonnull
	public PluralAttributeMapping getAttributeMapping() {
		return attributeMapping;
	}

	@Nonnull
	public SelectStatement getSqlAst() {
		return sqlAst;
	}

	@Nonnull
	public JdbcParametersList getJdbcParameters() {
		return jdbcParameters;
	}

	@Nullable
	public Object load(@Nonnull Object key, @Nonnull Object index, @Nonnull SharedSessionContractImplementor session) {
		final var jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				key,
				attributeMapping.getKeyDescriptor(),
				jdbcParameters,
				session
		);
		try {
			offset += indexRestriction.bind(
					jdbcParameterBindings,
					incrementIndexByBase( index ),
					offset,
					jdbcParameters,
					session
			);
		}
		catch (TransientObjectException e) {
			// an unsaved entity is not a key of the map
			return null;
		}
		assert offset == jdbcParameters.size();

		final var list = session.getJdbcServices().getJdbcSelectExecutor().list(
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
	@Nonnull
	protected Object incrementIndexByBase(@Nonnull Object index) {
		if ( baseIndex > 0 ) {
			return (Integer) index + baseIndex;
		}
		return index;
	}
}
