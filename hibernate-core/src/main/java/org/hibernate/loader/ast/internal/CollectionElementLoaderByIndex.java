/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.Loader;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;

/**
 * @author Andrea Boriero
 */
public class CollectionElementLoaderByIndex implements Loader {
	private final PluralAttributeMapping attributeMapping;
	private final SelectStatement sqlAst;
	private final List<JdbcParameter> jdbcParameters;
	private final int baseIndex;

	private final int keyJdbcCount;

	public CollectionElementLoaderByIndex(
			PluralAttributeMapping attributeMapping,
			int baseIndex,
			LoadQueryInfluencers influencers,
			SessionFactoryImplementor sessionFactory) {
		this.attributeMapping = attributeMapping;
		this.baseIndex = baseIndex;

		final ForeignKeyDescriptor keyDescriptor = attributeMapping.getKeyDescriptor();
		final CollectionPart indexDescriptor = attributeMapping.getIndexDescriptor();

		List<ModelPart> restrictedParts = new ArrayList<>();
		restrictedParts.add( keyDescriptor );

		if ( indexDescriptor instanceof EntityCollectionPart ) {
			EntityIdentifierMapping identifierMapping = ( (EntityCollectionPart) indexDescriptor ).getEntityMappingType()
					.getIdentifierMapping();
			restrictedParts.add( identifierMapping );
			this.keyJdbcCount = keyDescriptor.getJdbcTypeCount( sessionFactory.getTypeConfiguration() ) +
					identifierMapping.getJdbcTypeCount( sessionFactory.getTypeConfiguration() );
		}
		else {
			restrictedParts.add( indexDescriptor );
			this.keyJdbcCount = keyDescriptor.getJdbcTypeCount( sessionFactory.getTypeConfiguration() ) +
					indexDescriptor.getJdbcTypeCount( sessionFactory.getTypeConfiguration() );
		}

		List<ModelPart> partsToSelect = new ArrayList<>();
		partsToSelect.add( attributeMapping.getElementDescriptor() );

		this.jdbcParameters = new ArrayList<>( keyJdbcCount );
		this.sqlAst = LoaderSelectBuilder.createSelect(
				attributeMapping,
				partsToSelect,
				restrictedParts,
				null,
				1,
				influencers,
				LockOptions.READ,
				jdbcParameters::add,
				sessionFactory
		);
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

	public List<JdbcParameter> getJdbcParameters() {
		return jdbcParameters;
	}

	public Object load(Object key, Object index, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory )
				.translate( sqlAst );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( keyJdbcCount );
		jdbcSelect.bindFilterJdbcParameters( jdbcParameterBindings );

		final Iterator<JdbcParameter> paramItr = jdbcParameters.iterator();

		attributeMapping.getKeyDescriptor().visitJdbcValues(
				key,
				Clause.WHERE,
				(value, type) -> {
					assert paramItr.hasNext();
					final JdbcParameter parameter = paramItr.next();
					jdbcParameterBindings.addBinding(
							parameter,
							new JdbcParameterBindingImpl( type, value )
					);
				},
				session
		);
		attributeMapping.getIndexDescriptor().visitJdbcValues(
				incrementIndexByBase( index ),
				Clause.WHERE,
				(value, type) -> {
					assert paramItr.hasNext();
					final JdbcParameter parameter = paramItr.next();
					jdbcParameterBindings.addBinding(
							parameter,
							new JdbcParameterBindingImpl( type, value )
					);
				},
				session
		);
		assert !paramItr.hasNext();

		List<Object> list = jdbcServices.getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public CollectionKey getCollectionKey() {
						return null;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return QueryOptions.NONE;
					}

					@Override
					public QueryParameterBindings getQueryParameterBindings() {
						return QueryParameterBindings.NO_PARAM_BINDINGS;
					}

					@Override
					public Callback getCallback() {
						return null;
					}
				},
				RowTransformerPassThruImpl.instance(),
				true
		);

		if ( list.isEmpty() ) {
			return null;
		}
		return list.get( 0 );
	}

	protected Object incrementIndexByBase(Object index) {
		if ( baseIndex != 0 ) {
			index = (Integer) index + baseIndex;
		}
		return index;
	}
}
