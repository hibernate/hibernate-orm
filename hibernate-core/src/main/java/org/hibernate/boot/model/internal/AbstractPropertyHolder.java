/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.usertype.internal.AbstractTimeZoneStorageCompositeUserType;
import org.hibernate.usertype.internal.OffsetTimeCompositeUserType;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

import static java.util.Collections.emptyMap;
import static org.hibernate.boot.model.internal.TimeZoneStorageHelper.isOffsetTimeClass;
import static org.hibernate.boot.model.internal.TimeZoneStorageHelper.useColumnForTimeZoneStorage;
import static org.hibernate.internal.util.StringHelper.isNotBlank;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * @author Emmanuel Bernard
 */
public abstract class AbstractPropertyHolder implements PropertyHolder {

	private final String path;
	protected final AbstractPropertyHolder parent;
	protected final MetadataBuildingContext context;

	private Boolean isInIdClass;

	private Map<String, Column[]> holderColumnOverride;
	private Map<String, Column[]> currentPropertyColumnOverride;
	private Map<String, ColumnTransformer> holderColumnTransformerOverride;
	private Map<String, ColumnTransformer> currentPropertyColumnTransformerOverride;
	private Map<String, JoinColumn[]> holderJoinColumnOverride;
	private Map<String, JoinColumn[]> currentPropertyJoinColumnOverride;
	private Map<String, JoinTable> holderJoinTableOverride;
	private Map<String, JoinTable> currentPropertyJoinTableOverride;
	private Map<String, ForeignKey> holderForeignKeyOverride;
	private Map<String, ForeignKey> currentPropertyForeignKeyOverride;

	AbstractPropertyHolder(
			String path,
			PropertyHolder parent,
			ClassDetails clazzToProcess,
			MetadataBuildingContext context) {
		this.path = path;
		this.parent = (AbstractPropertyHolder) parent;
		this.context = context;
		buildHierarchyColumnOverride( clazzToProcess );
	}

	protected abstract String normalizeCompositePathForLogging(String attributeName);
	protected abstract String normalizeCompositePath(String attributeName);

	protected abstract AttributeConversionInfo locateAttributeConversionInfo(MemberDetails attributeMember);
	protected abstract AttributeConversionInfo locateAttributeConversionInfo(String path);

	@Override
	public ConverterDescriptor<?,?> resolveAttributeConverterDescriptor(MemberDetails attributeMember, boolean autoApply) {
		final var info = locateAttributeConversionInfo( attributeMember );
		if ( info != null ) {
			if ( info.isConversionDisabled() ) {
				return null;
			}
			else {
				try {
					return makeAttributeConverterDescriptor( info );
				}
				catch (Exception e) {
					throw buildExceptionFromInstantiationError( info, e );
				}
			}
		}
		else {
			return autoApply
					? autoApplyConverterForAttribute( attributeMember )
					: null;
		}
	}

	private ConverterDescriptor<?, ?> autoApplyConverterForAttribute(MemberDetails attributeMember) {
		return context.getMetadataCollector().getConverterRegistry()
				.getAttributeConverterAutoApplyHandler()
				.findAutoApplyConverterForAttribute( attributeMember, context );
	}

	protected IllegalStateException buildExceptionFromInstantiationError(AttributeConversionInfo info, Exception e) {
		if ( AttributeConverter.class.equals( info.getConverterClass() ) ) {
			// the user forgot to set @Convert.converter
			// we already know it's not a @Convert.disableConversion
			return new IllegalStateException(
					"Unable to instantiate AttributeConverter because the 'converter' member of '@Convert' was not specified",
					e
			);

		}
		else {
			return new IllegalStateException(
					String.format(
							"Unable to instantiate AttributeConverter [%s]",
							info.getConverterClass().getName()
					),
					e
			);
		}
	}

	protected ConverterDescriptor<?,?> makeAttributeConverterDescriptor(AttributeConversionInfo conversion) {
		try {
			return ConverterDescriptors.of( conversion.getConverterClass(), null, false );
		}
		catch (Exception e) {
			throw new AnnotationException( "Unable to create AttributeConverter instance", e );
		}
	}

	@Override
	public boolean isInIdClass() {
		if ( isInIdClass != null ) {
			return isInIdClass;
		}
		else if ( parent != null ) {
			return parent.isInIdClass();
		}
		else {
			return false;
		}
	}

	@Override
	public void setInIdClass(Boolean isInIdClass) {
		this.isInIdClass = isInIdClass;
	}

	@Override
	public String getPath() {
		return path;
	}

	/**
	 * Get the mappings
	 *
	 * @return The mappings
	 */
	protected MetadataBuildingContext getContext() {
		return context;
	}

	protected ModelsContext getSourceModelContext() {
		return getContext().getBootstrapContext().getModelsContext();
	}

	/**
	 * Set the property to be processed.  property can be null
	 *
	 * @param attributeMember The property
	 */
	protected void setCurrentProperty(MemberDetails attributeMember) {
		// todo (jpa32) : some of this (association override handling esp) does the same work multiple times - consolidate

		if ( attributeMember == null ) {
			currentPropertyColumnOverride = null;
			currentPropertyColumnTransformerOverride = null;
			currentPropertyJoinColumnOverride = null;
			currentPropertyJoinTableOverride = null;
			currentPropertyForeignKeyOverride = null;
		}
		else {
			currentPropertyColumnOverride = buildColumnOverride( attributeMember, getPath(), context );
			if ( currentPropertyColumnOverride.isEmpty() ) {
				currentPropertyColumnOverride = null;
			}

			currentPropertyColumnTransformerOverride = buildColumnTransformerOverride( attributeMember, context );
			if ( currentPropertyColumnTransformerOverride.isEmpty() ) {
				currentPropertyColumnTransformerOverride = null;
			}

			currentPropertyJoinColumnOverride = buildJoinColumnOverride( attributeMember, getPath(), context );
			if ( currentPropertyJoinColumnOverride.isEmpty() ) {
				currentPropertyJoinColumnOverride = null;
			}

			currentPropertyJoinTableOverride = buildJoinTableOverride( attributeMember, getPath(), context );
			if ( currentPropertyJoinTableOverride.isEmpty() ) {
				currentPropertyJoinTableOverride = null;
			}

			currentPropertyForeignKeyOverride = buildForeignKeyOverride( attributeMember, getPath(), context );
			if ( currentPropertyForeignKeyOverride.isEmpty() ) {
				currentPropertyForeignKeyOverride = null;
			}
		}
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&amp;&amp;element' with nothing
	 * <p>
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 */
	@Override
	public Column[] getOverriddenColumn(String propertyName) {
		final var overriddenColumn = getExactOverriddenColumn( propertyName );
		// support for non-map collections where no prefix is needed
		return overriddenColumn == null && propertyName.contains( ".{element}." )
				? getExactOverriddenColumn( propertyName.replace( ".{element}.", "." ) )
				: overriddenColumn;
	}

	@Override
	public ColumnTransformer getOverriddenColumnTransformer(String logicalColumnName) {
		if ( parent != null ) {
			final var result = parent.getOverriddenColumnTransformer( logicalColumnName );
			if ( result != null ) {
				return result;
			}
		}

		if ( currentPropertyColumnTransformerOverride != null ) {
			final var result = currentPropertyColumnTransformerOverride.get( logicalColumnName );
			if ( result != null ) {
				return result;
			}
		}

		if ( holderColumnTransformerOverride != null ) {
			final var result = holderColumnTransformerOverride.get( logicalColumnName );
			if ( result != null ) {
				return result;
			}
		}

		return null;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * find the overridden rules from the exact property name.
	 */
	private Column[] getExactOverriddenColumn(String propertyName) {
		if ( parent != null ) {
			final var result = parent.getExactOverriddenColumn( propertyName );
			if ( result != null ) {
				return result;
			}
		}

		if ( currentPropertyColumnOverride != null ) {
			final var result = currentPropertyColumnOverride.get( propertyName );
			if ( result != null ) {
				return result;
			}
		}

		if ( holderColumnOverride != null ) {
			final var result = holderColumnOverride.get( propertyName );
			if ( result != null ) {
				return result;
			}
		}

		return null;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&amp;&amp;element' with nothing
	 * <p>
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 */
	@Override
	public JoinColumn[] getOverriddenJoinColumn(String propertyName) {
		final var result = getExactOverriddenJoinColumn( propertyName );
		if ( result == null && propertyName.contains( ".{element}." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			return getExactOverriddenJoinColumn( propertyName.replace( ".{element}.", "."  ) );
		}
		else {
			return result;
		}
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 */
	private JoinColumn[] getExactOverriddenJoinColumn(String propertyName) {
		if ( parent != null ) {
			final var result = parent.getExactOverriddenJoinColumn( propertyName );
			if ( result != null ) {
				return result;
			}
		}

		if ( currentPropertyJoinColumnOverride != null ) {
			final var result = currentPropertyJoinColumnOverride.get( propertyName );
			if ( result != null ) {
				return result;
			}
		}

		if ( holderJoinColumnOverride != null ) {
			final var result = holderJoinColumnOverride.get( propertyName );
			if ( result != null ) {
				return result;
			}
		}

		return null;
	}

	@Override
	public ForeignKey getOverriddenForeignKey(String propertyName) {
		final var result = getExactOverriddenForeignKey( propertyName );
		if ( result == null && propertyName.contains( ".{element}." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			return getExactOverriddenForeignKey( propertyName.replace( ".{element}.", "." ) );
		}
		else {
			return result;
		}
	}

	private ForeignKey getExactOverriddenForeignKey(String propertyName) {
		if ( parent != null ) {
			final var result = parent.getExactOverriddenForeignKey( propertyName );
			if ( result != null ) {
				return result;
			}
		}
		if ( currentPropertyForeignKeyOverride != null ) {
			final var result = currentPropertyForeignKeyOverride.get( propertyName );
			if ( result != null ) {
				return result;
			}
		}
		if ( holderForeignKeyOverride != null ) {
			final var result = holderForeignKeyOverride.get( propertyName );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&amp;&amp;element' with nothing
	 * <p>
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 */
	@Override
	public JoinTable getJoinTable(MemberDetails attributeMember) {
		final String propertyName = qualify( getPath(), attributeMember.getName() );
		final var result = getOverriddenJoinTable( propertyName );
		return result == null
				? attributeMember.getDirectAnnotationUsage( JoinTable.class )
				: result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&amp;&amp;element' with nothing
	 *
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 */
	public JoinTable getOverriddenJoinTable(String propertyName) {
		final var result = getExactOverriddenJoinTable( propertyName );
		if ( result == null && propertyName.contains( ".{element}." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			return getExactOverriddenJoinTable( propertyName.replace( ".{element}.", "."  ) );
		}
		else {
			return result;
		}
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 */
	private JoinTable getExactOverriddenJoinTable(String propertyName) {
		if ( parent != null ) {
			final var override = parent.getExactOverriddenJoinTable( propertyName );
			if ( override != null ) {
				return override;
			}
		}
		if ( currentPropertyJoinTableOverride != null ) {
			final var override = currentPropertyJoinTableOverride.get( propertyName );
			if ( override != null ) {
				return override;
			}
		}
		if ( holderJoinTableOverride != null ) {
			final var override = holderJoinTableOverride.get( propertyName );
			if ( override != null ) {
				return override;
			}
		}
		return null;
	}

	private void buildHierarchyColumnOverride(ClassDetails element) {
		ClassDetails current = element;
		Map<String, Column[]> columnOverride = new HashMap<>();
		Map<String, ColumnTransformer> columnTransformerOverride = new HashMap<>();
		Map<String, JoinColumn[]> joinColumnOverride = new HashMap<>();
		Map<String, JoinTable> joinTableOverride = new HashMap<>();
		Map<String, ForeignKey> foreignKeyOverride = new HashMap<>();
		while ( current != null && !ClassDetails.OBJECT_CLASS_DETAILS.equals( current ) ) {
			if ( isManagedType( current ) ) {
				//FIXME is embeddable override?
				final var currentOverride = buildColumnOverride( current, path, context );
				var currentTransformerOverride = buildColumnTransformerOverride( current, context );
				final var currentJoinOverride = buildJoinColumnOverride( current, path, context );
				final var currentJoinTableOverride = buildJoinTableOverride( current, path, context );
				final var currentForeignKeyOverride = buildForeignKeyOverride( current, path, context );
				currentOverride.putAll( columnOverride ); //subclasses have precedence over superclasses
				currentTransformerOverride.putAll( columnTransformerOverride ); //subclasses have precedence over superclasses
				currentJoinOverride.putAll( joinColumnOverride ); //subclasses have precedence over superclasses
				currentJoinTableOverride.putAll( joinTableOverride ); //subclasses have precedence over superclasses
				currentForeignKeyOverride.putAll( foreignKeyOverride ); //subclasses have precedence over superclasses
				columnOverride = currentOverride;
				columnTransformerOverride = currentTransformerOverride;
				joinColumnOverride = currentJoinOverride;
				joinTableOverride = currentJoinTableOverride;
				foreignKeyOverride = currentForeignKeyOverride;
			}
			current = current.getSuperClass();
		}

		holderColumnOverride = columnOverride.isEmpty() ? null : columnOverride;
		holderColumnTransformerOverride = columnTransformerOverride.isEmpty() ? null : columnTransformerOverride;
		holderJoinColumnOverride = joinColumnOverride.isEmpty() ? null : joinColumnOverride;
		holderJoinTableOverride = joinTableOverride.isEmpty() ? null : joinTableOverride;
		holderForeignKeyOverride = foreignKeyOverride.isEmpty() ? null : foreignKeyOverride;
	}

	private static boolean isManagedType(ClassDetails current) {
		return EntityBinder.isEntity( current )
			|| EntityBinder.isMappedSuperclass( current )
			|| EmbeddableBinder.isEmbeddable( current );
	}

	private static Map<String, Column[]> buildColumnOverride(
			AnnotationTarget element,
			String path,
			MetadataBuildingContext context) {
		final Map<String,Column[]> result = new HashMap<>();
		if ( element == null ) {
			return result;
		}

		final var overrides =
				element.getRepeatedAnnotationUsages( AttributeOverride.class,
						context.getBootstrapContext().getModelsContext() );
		final Map<String, List<Column>> columnOverrideMap = new HashMap<>();
		if ( isNotEmpty( overrides ) ) {
			for ( var depAttr : overrides ) {
				final String qualifiedName = qualify( path, depAttr.name() );
				if ( !columnOverrideMap.containsKey( qualifiedName ) ) {
					columnOverrideMap.put( qualifiedName, new ArrayList<>() );
				}
				columnOverrideMap.get( qualifiedName ).add( depAttr.column() );
			}
		}
		else if ( useColumnForTimeZoneStorage( element, context ) ) {
			final var column = createTemporalColumn( element, path, context );
			if ( isOffsetTimeClass( element ) ) {
				columnOverrideMap.put(
						path + "." + OffsetTimeCompositeUserType.LOCAL_TIME_NAME,
						List.of( column )
				);
			}
			else {
				columnOverrideMap.put(
						path + "." + AbstractTimeZoneStorageCompositeUserType.INSTANT_NAME,
						List.of( column )
				);
			}
			final var offsetColumn = createTimeZoneColumn( element, column, context );
			columnOverrideMap.put(
					path + "." + AbstractTimeZoneStorageCompositeUserType.ZONE_OFFSET_NAME,
					List.of( offsetColumn )
			);
		}

		columnOverrideMap.forEach( (name, columns) -> {
			result.put( name, columns.toArray(new Column[0]) );
		} );
		return result;
	}

	private static Column createTimeZoneColumn(
			AnnotationTarget element,
			Column column,
			MetadataBuildingContext context) {
		final var timeZoneColumn = element.getDirectAnnotationUsage( TimeZoneColumn.class );
		final var created =
				JpaAnnotations.COLUMN.createUsage( context.getBootstrapContext().getModelsContext() );
		final String columnName =
				timeZoneColumn != null
						? timeZoneColumn.name()
						: column.name() + "_tz";
		created.name( columnName );
		created.nullable( column.nullable() );

		if ( timeZoneColumn != null ) {
			created.options( timeZoneColumn.options() );
			created.comment( timeZoneColumn.comment() );
			created.table( timeZoneColumn.table() );
			created.insertable( timeZoneColumn.insertable() );
			created.updatable( timeZoneColumn.updatable() );
			created.columnDefinition( timeZoneColumn.columnDefinition() );
		}
		else {
			created.table( column.table() );
			created.insertable( column.insertable() );
			created.updatable( column.updatable() );
			created.columnDefinition( column.columnDefinition() );
			created.options( column.options() );
			created.comment( column.comment() );
		}

		return created;
	}

	private static Column createTemporalColumn(
			AnnotationTarget element,
			String path,
			MetadataBuildingContext context) {
		int precision;
		int secondPrecision;
		final var annotatedColumn = element.getDirectAnnotationUsage( Column.class );
		if ( annotatedColumn != null ) {
			if ( isNotBlank( annotatedColumn.name() ) ) {
				return annotatedColumn;
			}
			precision = annotatedColumn.precision();
			secondPrecision = annotatedColumn.secondPrecision();
		}
		else {
			precision = 0;
			secondPrecision = -1;
		}

		// Base the name of the synthetic dateTime field on the name of the original attribute
		final Identifier implicitName = context.getObjectNameNormalizer().normalizeIdentifierQuoting(
				context.getBuildingOptions().getImplicitNamingStrategy().determineBasicColumnName(
						new ImplicitBasicColumnNameSource() {
							final AttributePath attributePath = AttributePath.parse(path);

							@Override
							public AttributePath getAttributePath() {
								return attributePath;
							}

							@Override
							public boolean isCollectionElement() {
								return false;
							}

							@Override
							public MetadataBuildingContext getBuildingContext() {
								return context;
							}
						}
				)
		);

		final var created =
				JpaAnnotations.COLUMN.createUsage( context.getBootstrapContext().getModelsContext() );
		if ( StringHelper.isNotEmpty( implicitName.getText() ) ) {
			created.name( implicitName.getText() );
		}
		created.precision( precision );
		created.secondPrecision( secondPrecision );
		return created;
	}

	private static Map<String, ColumnTransformer> buildColumnTransformerOverride(AnnotationTarget element, MetadataBuildingContext context) {
		if ( element == null ) {
			return emptyMap();
		}
		else {
			final Map<String, ColumnTransformer> columnOverride = new HashMap<>();
			final var sourceModelContext = context.getBootstrapContext().getModelsContext();
			element.forEachAnnotationUsage( ColumnTransformer.class, sourceModelContext,
					usage -> columnOverride.put( usage.forColumn(), usage ) );
			return columnOverride;
		}
	}

	private static Map<String, JoinColumn[]> buildJoinColumnOverride(AnnotationTarget element, String path, MetadataBuildingContext context) {
		if ( element == null ) {
			return emptyMap();
		}
		else {
			final Map<String, JoinColumn[]> columnOverride = new HashMap<>();
			for ( var override : buildAssociationOverrides( element, context ) ) {
				columnOverride.put( qualify( path, override.name() ), override.joinColumns() );
			}
			return columnOverride;
		}
	}

	private static Map<String, ForeignKey> buildForeignKeyOverride(AnnotationTarget element, String path, MetadataBuildingContext context) {
		if ( element == null ) {
			return emptyMap();
		}
		else {
			final Map<String, ForeignKey> foreignKeyOverride = new HashMap<>();
			for ( var override : buildAssociationOverrides( element, context ) ) {
				foreignKeyOverride.put( qualify( path, override.name() ), override.foreignKey() );
			}
			return foreignKeyOverride;
		}
	}

	private static AssociationOverride[] buildAssociationOverrides(AnnotationTarget element, MetadataBuildingContext context) {
		return element.getRepeatedAnnotationUsages( AssociationOverride.class, context.getBootstrapContext().getModelsContext() );
	}

	private static Map<String, JoinTable> buildJoinTableOverride(AnnotationTarget element, String path, MetadataBuildingContext context) {
		if ( element == null ) {
			return emptyMap();
		}
		else {
			final Map<String, JoinTable> result = new HashMap<>();
			for ( var override : buildAssociationOverrides( element, context ) ) {
				if ( isEmpty( override.joinColumns() ) ) {
					result.put( qualify( path, override.name() ), override.joinTable() );
				}
			}
			return result;
		}
	}

	@Override
	public void setParentProperty(String parentProperty) {
		throw new AssertionFailure( "Setting the parent property to a non component" );
	}
}
