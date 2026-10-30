package org.hibernate.sql.results.graph.collection.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.FetchOptions;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.internal.LoadingCollectionEntryImpl;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.format.JsonDocumentItemType;
import org.hibernate.type.format.JsonDocumentReader;
import org.hibernate.type.format.JsonValueJDBCTypeAdapter;
import org.hibernate.type.format.JsonValueJDBCTypeAdapterFactory;
import org.hibernate.type.format.StringJsonDocumentReader;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * Initializes a collection fetched {@linkplain NestedCollectionFetch nested}
 * from the aggregated value in the row of the owner. For every row of the nested collection,
 * the element initializers are driven through a {@link NestedCollectionRowProcessingState},
 * just as the {@link org.hibernate.sql.results.spi.RowReader} drives the initializers
 * for every row of the result set, and the element is added to the loading collection.
 *
 * @author Khurelkhuyag
 *
 * @since 8.1
 */
public class NestedCollectionInitializer
		extends AbstractNonJoinCollectionInitializer<NestedCollectionInitializer.NestedCollectionInitializerData> {

	private final SqlSelection aggregateSelection;
	private final JdbcMapping[] rowJdbcMappings;
	private final DomainResultAssembler<?> elementAssembler;
	private final @Nullable DomainResultAssembler<?> indexAssembler;
	private final CollectionClassification classification;
	private final int listIndexBase;
	private final Initializer<InitializerData>[] elementInitializers;
	private final Initializer<InitializerData>[] elementRootInitializers;
	private final Initializer<InitializerData>[] elementInitializersInResolveOrder;

	public static class NestedCollectionInitializerData extends AbstractCollectionInitializer.CollectionInitializerData {
		private final NestedCollectionRowProcessingState elementRowProcessingState;

		public NestedCollectionInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
			elementRowProcessingState = new NestedCollectionRowProcessingState( rowProcessingState );
		}
	}

	public NestedCollectionInitializer(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedMapping,
			InitializerParent<?> parent,
			@Nullable DomainResult<?> collectionKeyResult,
			SqlSelection aggregateSelection,
			List<SelectableMapping> rowSelectables,
			Fetch elementFetch,
			@Nullable Fetch indexFetch,
			FetchOptions fetchOptions,
			AssemblerCreationState creationState) {
		super( fetchedPath, fetchedMapping, parent, collectionKeyResult, false, fetchOptions, creationState );
		this.aggregateSelection = aggregateSelection;
		this.rowJdbcMappings = new JdbcMapping[rowSelectables.size()];
		for ( int i = 0; i < rowJdbcMappings.length; i++ ) {
			rowJdbcMappings[i] = rowSelectables.get( i ).getJdbcMapping();
		}
		this.classification = fetchedMapping.getMappedType().getCollectionSemantics().getCollectionClassification();
		final var indexMetadata = fetchedMapping.getIndexMetadata();
		this.listIndexBase = indexMetadata == null ? 0 : indexMetadata.getListIndexBase();

		final var elementCreationState = new NestedCollectionAssemblerCreationState( creationState );
		this.elementAssembler = elementFetch.createAssembler( this, elementCreationState );
		this.indexAssembler = indexFetch == null ? null : indexFetch.createAssembler( this, elementCreationState );

		final var initializers = elementCreationState.getInitializers();
		final List<Initializer<?>> roots = new ArrayList<>();
		final List<Initializer<?>> resolveOrder = new ArrayList<>( initializers.size() );
		for ( var initializer : initializers ) {
			if ( initializer.getParent() == this ) {
				roots.add( initializer );
			}
			// same order as InitializersList: entity initializers are resolved first
			if ( initializer.isEntityInitializer() ) {
				resolveOrder.add( initializer );
			}
		}
		for ( var initializer : initializers ) {
			if ( !initializer.isEntityInitializer() ) {
				resolveOrder.add( initializer );
			}
		}
		this.elementInitializers = toArray( initializers );
		this.elementRootInitializers = toArray( roots );
		this.elementInitializersInResolveOrder = toArray( resolveOrder );
	}

	@SuppressWarnings("unchecked")
	private static Initializer<InitializerData>[] toArray(List<Initializer<?>> initializers) {
		return initializers.toArray( new Initializer[0] );
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new NestedCollectionInitializerData( rowProcessingState );
	}

	@Override
	public void startLoading(RowProcessingState rowProcessingState) {
		super.startLoading( rowProcessingState );
		final var elementRowProcessingState = getData( rowProcessingState ).elementRowProcessingState;
		for ( var initializer : elementRootInitializers ) {
			initializer.startLoading( elementRowProcessingState );
		}
	}

	@Override
	public void resolveInstance(NestedCollectionInitializerData data) {
		resolveInstance( data, false );
		final var collection = data.getCollectionInstance();
		if ( collection != null && !collection.wasInitialized() ) {
			final var rowProcessingState = data.getRowProcessingState();
			final var collectionKey = data.collectionKey;
			assert collectionKey != null;
			final var loadContexts = rowProcessingState.getSession().getPersistenceContextInternal().getLoadContexts();
			if ( loadContexts.findLoadingCollectionEntry( collectionKey ) == null ) {
				final var loadingEntry = new LoadingCollectionEntryImpl(
						getCollectionAttributeMapping().getCollectionDescriptor(),
						this,
						collectionKey.getKey(),
						collection
				);
				rowProcessingState.getJdbcValuesSourceProcessingState()
						.registerLoadingCollection( collectionKey, loadingEntry );
				final Object json = rowProcessingState.getJdbcValue( aggregateSelection );
				if ( json != null ) {
					final var elementRowProcessingState = data.elementRowProcessingState;
					for ( var row : readRows( (String) json, rowProcessingState.getSession() ) ) {
						readElement( row, elementRowProcessingState, loadingEntry );
					}
					elementRowProcessingState.setCurrentRow( null );
				}
			}
		}
	}

	@Override
	public void resolveInstance(@Nullable Object instance, NestedCollectionInitializerData data) {
		resolveInstance( instance, data, true );
	}

	@Override
	public void resolveState(NestedCollectionInitializerData data) {
		super.resolveState( data );
		// the collection is not initialized from this row, but the aggregate must still be put in the query cache
		data.getRowProcessingState().getJdbcValue( aggregateSelection );
	}

	/**
	 * Reads {@code [[1,"a",null],[2,"b",3]]} into rows of JDBC values.
	 */
	private List<Object[]> readRows(String json, WrapperOptions options) {
		final var reader = new StringJsonDocumentReader( json );
		final var adapter = JsonValueJDBCTypeAdapterFactory.getAdapter( reader, false );
		final List<Object[]> rows = new ArrayList<>();
		try {
			if ( !reader.hasNext() || reader.next() != JsonDocumentItemType.ARRAY_START ) {
				throw new HibernateException( "Malformed nested collection, expected a JSON array: " + json );
			}
			while ( reader.hasNext() ) {
				final var item = reader.next();
				if ( item == JsonDocumentItemType.ARRAY_END ) {
					return rows;
				}
				else if ( item == JsonDocumentItemType.ARRAY_START ) {
					rows.add( readRow( reader, adapter, options ) );
				}
				else {
					throw new HibernateException( "Malformed nested collection, expected a JSON array per row: " + json );
				}
			}
			throw new HibernateException( "Malformed nested collection, expected the end of the JSON array: " + json );
		}
		catch (SQLException e) {
			throw new HibernateException( "Could not read nested collection: " + json, e );
		}
	}

	private Object[] readRow(JsonDocumentReader reader, JsonValueJDBCTypeAdapter adapter, WrapperOptions options)
			throws SQLException {
		final var row = new Object[rowJdbcMappings.length];
		for ( int i = 0; i < row.length; i++ ) {
			final var jdbcMapping = rowJdbcMappings[i];
			final var javaType = jdbcMapping.getJdbcJavaType();
			final var jdbcType = jdbcMapping.getJdbcType();
			row[i] = switch ( reader.next() ) {
				case NULL_VALUE -> null;
				case BOOLEAN_VALUE -> javaType.wrap( reader.getBooleanValue(), options );
				case NUMERIC_VALUE -> adapter.fromNumericValue( javaType, jdbcType, reader, options );
				case VALUE -> adapter.fromValue( javaType, jdbcType, reader, options );
				default -> throw new HibernateException( "Malformed nested collection, unexpected JSON value in row" );
			};
		}
		if ( reader.next() != JsonDocumentItemType.ARRAY_END ) {
			throw new HibernateException( "Malformed nested collection, unexpected number of values in row" );
		}
		return row;
	}

	/**
	 * Drives the element initializers over one row of the nested collection, the way
	 * {@code StandardRowReader} does over a row of the result set, and adds
	 * the element to the loading collection.
	 */
	private void readElement(
			Object[] row,
			NestedCollectionRowProcessingState elementRowProcessingState,
			LoadingCollectionEntryImpl loadingEntry) {
		elementRowProcessingState.setCurrentRow( row );
		for ( var initializer : elementRootInitializers ) {
			initializer.resolveKey( initializer.getData( elementRowProcessingState ) );
		}
		for ( var initializer : elementInitializersInResolveOrder ) {
			final var data = initializer.getData( elementRowProcessingState );
			if ( data.getState() == State.KEY_RESOLVED ) {
				initializer.resolveInstance( data );
			}
		}
		for ( var initializer : elementInitializers ) {
			final var data = initializer.getData( elementRowProcessingState );
			if ( data.getState() == State.RESOLVED ) {
				initializer.initializeInstance( data );
			}
		}
		final Object element = elementAssembler.assemble( elementRowProcessingState );
		final Object index = indexAssembler == null ? null : indexAssembler.assemble( elementRowProcessingState );
		for ( var initializer : elementInitializers ) {
			initializer.finishUpRow( initializer.getData( elementRowProcessingState ) );
		}
		if ( element != null ) {
			loadingEntry.load( loadingState -> {
				if ( classification == CollectionClassification.LIST ) {
					assert index != null;
					final int position = (Integer) index - listIndexBase;
					for ( int i = loadingState.size(); i <= position; i++ ) {
						loadingState.add( null );
					}
					loadingState.set( position, element );
				}
				else {
					loadingState.add( element );
				}
			} );
		}
	}

	@Override
	public void endLoading(NestedCollectionInitializerData data) {
		super.endLoading( data );
		for ( var initializer : elementInitializers ) {
			final var elementData = initializer.getData( data.elementRowProcessingState );
			if ( elementData != null ) {
				initializer.endLoading( elementData );
			}
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, NestedCollectionInitializerData data) {
		final Object instance = getInitializedPart().getValue( parentInstance );
		if ( instance == null ) {
			setMissing( data );
		}
		else {
			final var collection = getCollection( data, instance );
			data.setState( State.INITIALIZED );
			data.setCollectionInstance( collection );
			withFetchOptions(
					data.getRowProcessingState().getSession(),
					() -> {
						collection.forceInitialization();
						return null;
					}
			);
		}
	}

	@Override
	public String toString() {
		return "NestedCollectionInitializer(" + toLoggableString( getNavigablePath() ) + ")";
	}
}
