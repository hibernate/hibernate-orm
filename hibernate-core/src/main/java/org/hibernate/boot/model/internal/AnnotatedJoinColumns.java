/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.PropertyRef;
import org.hibernate.boot.internal.FailedSecondPassException;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitPrimaryKeyJoinColumnNameSource;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.JoinColumn;

import static org.hibernate.boot.model.internal.AnnotatedJoinColumn.buildExplicitJoinTableJoinColumn;
import static org.hibernate.boot.model.internal.AnnotatedJoinColumn.buildImplicitJoinTableJoinColumn;
import static org.hibernate.boot.model.internal.AnnotatedJoinColumn.buildJoinColumn;
import static org.hibernate.boot.model.internal.BinderHelper.findReferencedColumnOwner;
import static org.hibernate.boot.model.internal.BinderHelper.getRelativePath;
import static org.hibernate.boot.model.internal.ForeignKeyType.EXPLICIT_PRIMARY_KEY_REFERENCE;
import static org.hibernate.boot.model.internal.ForeignKeyType.NON_PRIMARY_KEY_REFERENCE;
import static org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION;
import static org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource.Nature.ENTITY;
import static org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource.Nature.ENTITY_COLLECTION;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.isNotBlank;
import static org.hibernate.internal.util.StringHelper.isQuoted;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.collections.ArrayHelper.isEmpty;

/**
 * A list of {@link jakarta.persistence.JoinColumn}s that form a single join
 * condition, similar in concept to {@link jakarta.persistence.JoinColumns},
 * but not every instance of this class corresponds to an explicit annotation
 * in the Java code.
 * <p>
 * There's no exact analog of this class in the mapping model, so some
 * information is lost when it's transformed into a list of {@link Column}s.
 *
 * @author Gavin King
 */
public class AnnotatedJoinColumns extends AnnotatedColumns {

	private final List<AnnotatedJoinColumn> columns = new ArrayList<>();

	private String referencedProperty;

	private String mappedBy;
	private String mapsId;
	//property name on the owning side if any
	private String mappedByPropertyName;
	//table name on the mapped by side if any
	private String mappedByTableName;
	private String mappedByEntityName;
	private boolean elementCollection;
	private String manyToManyOwnerSideEntityName;

	public static AnnotatedJoinColumns buildJoinColumnsOrFormulas(
			JoinColumnOrFormula[] joinColumnOrFormulas,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MetadataBuildingContext context) {
		final var parent = new AnnotatedJoinColumns();
		parent.setBuildingContext( context );
		parent.setJoins( joins );
		parent.setPropertyHolder( propertyHolder );
		parent.setPropertyName( getRelativePath( propertyHolder, inferredData.getPropertyName() ) );
		parent.setMappedBy( mappedBy );
		for ( var columnOrFormula : joinColumnOrFormulas ) {
			final var formula = columnOrFormula.formula();
			final var column = columnOrFormula.column();
			final String annotationString = formula.value();
			if ( isNotBlank( annotationString ) ) {
				AnnotatedJoinColumn.buildJoinFormula( formula, parent );
			}
			else {
				buildJoinColumn( column, mappedBy, parent, propertyHolder, inferredData );
			}
		}

		handlePropertyRef( inferredData.getAttributeMember(), parent );

		return parent;
	}

	private static void handlePropertyRef(MemberDetails attributeMember, AnnotatedJoinColumns parent) {
		final var propertyRefUsage = attributeMember.getDirectAnnotationUsage( PropertyRef.class );
		if ( propertyRefUsage != null ) {
			final String referencedPropertyName = propertyRefUsage.value();
			if ( isBlank( referencedPropertyName ) ) {
				throw new AnnotationException(
						"@PropertyRef did not specify target attribute name: " + attributeMember );
			}
			parent.referencedProperty = referencedPropertyName;
		}
	}

	static AnnotatedJoinColumns buildJoinColumnsWithFormula(
			JoinFormula joinFormula,
			Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MetadataBuildingContext context) {
		final var joinColumns = new AnnotatedJoinColumns();
		joinColumns.setBuildingContext( context );
		joinColumns.setJoins( secondaryTables );
		joinColumns.setPropertyHolder( propertyHolder );
		joinColumns.setPropertyName( getRelativePath( propertyHolder, inferredData.getPropertyName() ) );
		AnnotatedJoinColumn.buildJoinFormula( joinFormula, joinColumns );
		handlePropertyRef( inferredData.getAttributeMember(), joinColumns );
		return joinColumns;
	}

	public static AnnotatedJoinColumns buildJoinColumns(
			JoinColumn[] joinColumns,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MetadataBuildingContext buildingContext) {
		return buildJoinColumnsWithDefaultColumnSuffix(
				joinColumns,
				mappedBy,
				joins,
				propertyHolder,
				inferredData,
				"",
				buildingContext
		);
	}

	public static AnnotatedJoinColumns buildJoinColumnsWithDefaultColumnSuffix(
			JoinColumn[] joinColumns,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String defaultColumnSuffix,
			MetadataBuildingContext context) {
		assert mappedBy == null || !mappedBy.isBlank();
		final String propertyName = inferredData.getPropertyName();
		final String path = qualify( propertyHolder.getPath(), propertyName );
		final JoinColumn[] overrides = propertyHolder.getOverriddenJoinColumn( path );
		final JoinColumn[] actualColumns = overrides == null ? joinColumns : overrides;
		final var parent = new AnnotatedJoinColumns();
		parent.setBuildingContext( context );
		parent.setJoins( joins );
		parent.setPropertyHolder( propertyHolder );
		parent.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
		parent.setMappedBy( mappedBy );
		if ( isEmpty( actualColumns ) ) {
			buildJoinColumn(
					null,
					mappedBy,
					parent,
					propertyHolder,
					inferredData,
					defaultColumnSuffix
			);
		}
		else {
			parent.setMappedBy( mappedBy );
			for ( var actualColumn : actualColumns ) {
				buildJoinColumn(
						actualColumn,
						mappedBy,
						parent,
						propertyHolder,
						inferredData,
						defaultColumnSuffix
				);
			}
		}
		handlePropertyRef( inferredData.getAttributeMember(), parent );
		return parent;
	}

	/**
	 * Called for join tables in {@link jakarta.persistence.ManyToMany} associations.
	 */
	public static AnnotatedJoinColumns buildJoinTableJoinColumns(
			JoinColumn[] joinColumns,
			Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String mappedBy,
			MetadataBuildingContext context) {
		final var parent = new AnnotatedJoinColumns();
		parent.setBuildingContext( context );
		parent.setJoins( secondaryTables );
		parent.setPropertyHolder( propertyHolder );
		parent.setPropertyName( getRelativePath( propertyHolder, inferredData.getPropertyName() ) );
		parent.setMappedBy( mappedBy );
		if ( joinColumns == null ) {
			buildImplicitJoinTableJoinColumn( parent, propertyHolder, inferredData );
		}
		else {
			for ( var joinColumn : joinColumns ) {
				buildExplicitJoinTableJoinColumn( parent, propertyHolder, inferredData, joinColumn );
			}
		}
		handlePropertyRef( inferredData.getAttributeMember(), parent );
		return parent;
	}

	Property resolveMapsId() {
		final var persistentClass = getPropertyHolder().getPersistentClass();
		final var identifier = persistentClass.getIdentifier();
		try {
			return identifier instanceof Component embeddedIdType
					? embeddedIdType.getProperty( mapsId )   // an @EmbeddedId
					: persistentClass.getProperty( mapsId );  // a simple id or an @IdClass
		}
		catch (MappingException me) {
			throw new AnnotationException( "Identifier field '" + mapsId
					+ "' named in '@MapsId' does not exist in entity '" + persistentClass.getEntityName() + "'",
					me );
		}
	}

	public List<AnnotatedJoinColumn> getJoinColumns() {
		return columns;
	}

	@Override
	public void addColumn(AnnotatedColumn child) {
		if ( !( child instanceof AnnotatedJoinColumn joinColumn ) ) {
			throw new AssertionFailure( "wrong sort of column" );
		}
		addColumn( joinColumn );
	}

	public void addColumn(AnnotatedJoinColumn child) {
		super.addColumn( child );
		columns.add( child );
	}

	public String getReferencedProperty() {
		return referencedProperty;
	}

	public String getMappedBy() {
		return mappedBy;
	}

	public void setMappedBy(String mappedBy) {
		this.mappedBy = nullIfEmpty( mappedBy );
	}

	/**
	 * @return true if the association mapping annotation did specify
	 *         {@link jakarta.persistence.OneToMany#mappedBy() mappedBy},
	 *         meaning that this {@code @JoinColumn} mapping belongs to an
	 *         unowned many-valued association.
	 */
	public boolean hasMappedBy() {
		return mappedBy != null;
	}

	public String getMappedByEntityName() {
		return mappedByEntityName;
	}

	public String getMappedByPropertyName() {
		return mappedByPropertyName;
	}

	public String getMappedByTableName() {
		return mappedByTableName;
	}

	public boolean isElementCollection() {
		return elementCollection;
	}

	public void setElementCollection(boolean elementCollection) {
		this.elementCollection = elementCollection;
	}

	public void setManyToManyOwnerSideEntityName(String entityName) {
		manyToManyOwnerSideEntityName = entityName;
	}

	public String getManyToManyOwnerSideEntityName() {
		return manyToManyOwnerSideEntityName;
	}

	public void setMappedBy(String entityName, String logicalTableName, String mappedByProperty) {
		mappedByEntityName = entityName;
		mappedByTableName = logicalTableName;
		mappedByPropertyName = mappedByProperty;
	}

	/**
	 * Determine if the given {@link AnnotatedJoinColumns} represent a reference to
	 * the primary key of the given {@link PersistentClass}, or whether they reference
	 * some other combination of mapped columns.
	 */
	public ForeignKeyType getReferencedColumnsType(PersistentClass referencedEntity) {
		if ( referencedProperty != null ) {
			return NON_PRIMARY_KEY_REFERENCE;
		}

		if ( columns.isEmpty() ) {
			return ForeignKeyType.IMPLICIT_PRIMARY_KEY_REFERENCE; //shortcut
		}

		final var firstColumn = columns.get( 0 );
		final var context = getBuildingContext();
		final Object columnOwner = findReferencedColumnOwner( referencedEntity, firstColumn, context );
		if ( columnOwner == null ) {
			try {
				throw new MappingException( "A '@JoinColumn' references a column named '"
						+ firstColumn.getReferencedColumn() + "' but the target entity '"
						+ referencedEntity.getEntityName() + "' has no property which maps to this column" );
			}
			catch ( MappingException me ) {
				// we throw a recoverable exception here in case this
				// is merely an ordering issue, so that the SecondPass
				// will get reprocessed later
				throw new FailedSecondPassException( me.getMessage(), me );
			}
		}
		final var table = table( columnOwner );
//		final List<Selectable> keyColumns = referencedEntity.getKey().getSelectables();
		final var keyColumns =
				table.getPrimaryKey() == null
						? referencedEntity.getKey().getSelectables()
						: table.getPrimaryKey().getColumns();
		boolean explicitColumnReference = false;
		for ( var column : columns ) {
			if ( !column.isReferenceImplicit() ) {
				explicitColumnReference = true;
				if ( !keyColumns.contains( column( context, table, column.getReferencedColumn() ) ) ) {
					// we have a column which does not belong to the PK
					return NON_PRIMARY_KEY_REFERENCE;
				}
			}
		}
		if ( explicitColumnReference ) {
			// if we got to here, all the columns belong to the PK
			return keyColumns.size() == columns.size()
					// we have all the PK columns
					? EXPLICIT_PRIMARY_KEY_REFERENCE
					// we have a subset of the PK columns
					: NON_PRIMARY_KEY_REFERENCE;
		}
		else {
			// there were no nonempty referencedColumnNames
			return ForeignKeyType.IMPLICIT_PRIMARY_KEY_REFERENCE;
		}
	}

	private static Table table(Object persistentClassOrJoin) {
		if ( persistentClassOrJoin instanceof PersistentClass persistentClass ) {
			return persistentClass.getTable();
		}
		else if ( persistentClassOrJoin instanceof Join join ) {
			return join.getTable();
		}
		else {
			throw new IllegalArgumentException( "Unexpected object" );
		}
	}

	private static Column column(MetadataBuildingContext context, Table table, String logicalReferencedColumnName) {
		try {
			return new Column( context.getMetadataCollector()
					.getPhysicalColumnName( table, logicalReferencedColumnName ) );
		}
		catch ( MappingException me ) {
			throw new MappingException( "No column with logical name '" + logicalReferencedColumnName
					+ "' in table '" + table.getName() + "'" );
		}
	}

	String buildDefaultColumnName(PersistentClass referencedEntity, String logicalReferencedColumn) {
		final var context = getBuildingContext();
		final var options = context.getBuildingOptions();
		final var collector = context.getMetadataCollector();
		final var database = collector.getDatabase();
		final var jdbcEnvironment = database.getJdbcEnvironment();
		final Identifier columnIdentifier = columnIdentifier(
				referencedEntity,
				logicalReferencedColumn,
				options.getImplicitNamingStrategy(),
				collector,
				database
		);
		return options.getPhysicalNamingStrategy()
				.toPhysicalColumnName( columnIdentifier, jdbcEnvironment )
				.render( jdbcEnvironment.getDialect() );
	}

	private Identifier columnIdentifier(
			PersistentClass referencedEntity,
			String logicalReferencedColumn,
			ImplicitNamingStrategy implicitNamingStrategy,
			InFlightMetadataCollector collector,
			Database database) {
		final boolean isRefColumnQuoted = isQuoted( logicalReferencedColumn );

		if ( isMappedBySide() ) {
			// NOTE: An @ElementCollection can't be mappedBy, but the client code
			//       also handles the inverse side of many-to-many mappings
			final Identifier columnIdentifier = implicitNamingStrategy.determineJoinColumnName(
					new UnownedImplicitJoinColumnNameSource( referencedEntity, logicalReferencedColumn )
			);
			//one element was quoted so we quote
			return quoteIfNecessary( isRefColumnQuoted, getMappedByTableName(), columnIdentifier );
		}
		else if ( isOwnerSide() ) {
			final String logicalTableName = collector.getLogicalTableName( referencedEntity.getTable() );
			final Identifier columnIdentifier = implicitNamingStrategy.determineJoinColumnName(
					new OwnedImplicitJoinColumnNameSource( referencedEntity, logicalTableName, logicalReferencedColumn )
			);
			return quoteIfNecessary( isRefColumnQuoted, logicalTableName, handleElement( columnIdentifier ) );
		}
		else {
			final Identifier logicalTableName = database.toIdentifier(
					collector.getLogicalTableName( referencedEntity.getTable() )
			);
			// is an intra-entity hierarchy table join so copy the name by default
			final Identifier columnIdentifier = implicitNamingStrategy.determinePrimaryKeyJoinColumnName(
					new ImplicitPrimaryKeyJoinColumnNameSource() {
						@Override
						public MetadataBuildingContext getBuildingContext() {
							return AnnotatedJoinColumns.this.getBuildingContext();
						}

						@Override
						public Identifier getReferencedTableName() {
							return logicalTableName;
						}

						@Override
						public Identifier getReferencedPrimaryKeyColumnName() {
							return database.toIdentifier( logicalReferencedColumn );
						}
					}
			);
			return quoteIfNecessary( isRefColumnQuoted, logicalTableName, columnIdentifier );
		}
	}

	private Identifier handleElement(Identifier columnIdentifier) {
		// HHH-11826 magic. See AnnotatedColumn and the HHH-6005 comments
		final String identifierText = columnIdentifier.getText();
		if ( identifierText.contains( "_{element}_" ) ) {
			return Identifier.toIdentifier(
					identifierText.replace( "_{element}_", "_" ),
					columnIdentifier.isQuoted()
			);
		}
		else {
			return columnIdentifier;
		}
	}

	private static Identifier quoteIfNecessary(
			boolean isRefColumnQuoted, Identifier logicalTableName, Identifier columnIdentifier) {
		return !columnIdentifier.isQuoted()
			&& ( isRefColumnQuoted || logicalTableName.isQuoted() )
				? columnIdentifier.quoted()
				: columnIdentifier;
	}

	private static Identifier quoteIfNecessary(
			boolean isRefColumnQuoted, String logicalTableName, Identifier columnIdentifier) {
		//one element was quoted so we quote
		return isRefColumnQuoted || isQuoted( logicalTableName )
				? columnIdentifier.quoted()
				: columnIdentifier;
	}

	private boolean isOwnerSide() {
		return getPropertyName() != null;
	}

	private boolean isMappedBySide() {
		return getMappedByTableName() != null
			|| getMappedByPropertyName() != null;
	}

	private ImplicitJoinColumnNameSource.Nature getImplicitNature() {
		if ( getPropertyHolder().isEntity() ) {
			return ENTITY;
		}
		else if ( isElementCollection() ) {
			return ELEMENT_COLLECTION;
		}
		else {
			return ENTITY_COLLECTION;
		}
	}

	public boolean hasMapsId() {
		return mapsId != null;
	}

	public String getMapsId() {
		return mapsId;
	}

	public void setMapsId(String mapsId) {
		this.mapsId = nullIfEmpty( mapsId );
	}

	private class UnownedImplicitJoinColumnNameSource implements ImplicitJoinColumnNameSource {
		final AttributePath attributePath;
		final Nature implicitNamingNature;

		private final EntityNaming entityNaming;

		private final Identifier referencedTableName;
		private final String logicalReferencedColumn;

		final InFlightMetadataCollector collector = getBuildingContext().getMetadataCollector();
		final Database database = collector.getDatabase();

		public UnownedImplicitJoinColumnNameSource(PersistentClass referencedEntity, String logicalReferencedColumn) {
			this.logicalReferencedColumn = logicalReferencedColumn;
			attributePath = AttributePath.parse( getMappedByPropertyName() );
			implicitNamingNature = getImplicitNature();
			entityNaming = new EntityNaming() {
				@Override
				public String getClassName() {
					return referencedEntity.getClassName();
				}

				@Override
				public String getEntityName() {
					return referencedEntity.getEntityName();
				}

				@Override
				public String getJpaEntityName() {
					return referencedEntity.getJpaEntityName();
				}
			};
			referencedTableName = database.toIdentifier( getMappedByTableName() );
		}

		@Override
		public Nature getNature() {
			return implicitNamingNature;
		}

		@Override
		public EntityNaming getEntityNaming() {
			return entityNaming;
		}

		@Override
		public AttributePath getAttributePath() {
			return attributePath;
		}

		@Override
		public Identifier getReferencedTableName() {
			return referencedTableName;
		}

		@Override
		public Identifier getReferencedColumnName() {
			if ( logicalReferencedColumn != null ) {
				return database.toIdentifier( logicalReferencedColumn );
			}

			if ( getMappedByEntityName() == null || getMappedByPropertyName() == null ) {
				return null;
			}

			final var mappedByProperty =
					collector.getEntityBinding( getMappedByEntityName() )
							.getProperty( getMappedByPropertyName() );
			final var value = (SimpleValue) mappedByProperty.getValue();
			if ( value.getSelectables().isEmpty() ) {
				throw new AnnotationException(
						String.format(
								Locale.ENGLISH,
								"Association '%s' is 'mappedBy' a property '%s' of entity '%s' with no columns",
								getPropertyHolder().getPath(),
								getMappedByPropertyName(),
								getMappedByEntityName()
						)
				);
			}
			if ( !(value.getSelectables().get( 0 ) instanceof Column column) ) {
				throw new AnnotationException(
						String.format(
								Locale.ENGLISH,
								"Association '%s' is 'mappedBy' a property '%s' of entity '%s' which maps to a formula",
								getPropertyHolder().getPath(),
								getMappedByPropertyName(),
								getPropertyHolder().getPath()
						)
				);
			}
			if ( value.getSelectables().size() > 1 ) {
				throw new AnnotationException(
						String.format(
								Locale.ENGLISH,
								"Association '%s' is 'mappedBy' a property '%s' of entity '%s' with multiple columns",
								getPropertyHolder().getPath(),
								getMappedByPropertyName(),
								getPropertyHolder().getPath()
						)
				);
			}
			return column.getNameIdentifier( getBuildingContext() );
		}

		@Override
		public MetadataBuildingContext getBuildingContext() {
			return AnnotatedJoinColumns.this.getBuildingContext();
		}
	}

	private class OwnedImplicitJoinColumnNameSource implements ImplicitJoinColumnNameSource {
		final Nature implicitNamingNature;

		private final EntityNaming entityNaming;

		private final AttributePath attributePath;
		private final Identifier referencedTableName;
		private final Identifier referencedColumnName;

		final InFlightMetadataCollector collector = getBuildingContext().getMetadataCollector();
		final Database database = collector.getDatabase();

		public OwnedImplicitJoinColumnNameSource(PersistentClass referencedEntity, String logicalTableName, String logicalReferencedColumn) {
			implicitNamingNature = getImplicitNature();
			entityNaming = new EntityNaming() {
				@Override
				public String getClassName() {
					return referencedEntity.getClassName();
				}

				@Override
				public String getEntityName() {
					return referencedEntity.getEntityName();
				}

				@Override
				public String getJpaEntityName() {
					return referencedEntity.getJpaEntityName();
				}
			};
			attributePath = AttributePath.parse( getPropertyName() );
			referencedTableName = database.toIdentifier( logicalTableName );
			referencedColumnName = database.toIdentifier( logicalReferencedColumn );
		}

		@Override
		public Nature getNature() {
			return implicitNamingNature;
		}

		@Override
		public EntityNaming getEntityNaming() {
			return entityNaming;
		}

		@Override
		public AttributePath getAttributePath() {
			return attributePath;
		}

		@Override
		public Identifier getReferencedTableName() {
			return referencedTableName;
		}

		@Override
		public Identifier getReferencedColumnName() {
			return referencedColumnName;
		}

		@Override
		public MetadataBuildingContext getBuildingContext() {
			return AnnotatedJoinColumns.this.getBuildingContext();
		}
	}
}
