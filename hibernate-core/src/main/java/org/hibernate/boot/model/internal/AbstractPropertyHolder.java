/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.usertype.internal.AbstractTimeZoneStorageCompositeUserType;
import org.hibernate.usertype.internal.OffsetTimeCompositeUserType;

import org.jboss.logging.Logger;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MappedSuperclass;

import static org.hibernate.boot.model.internal.TimeZoneStorageHelper.isOffsetTimeClass;
import static org.hibernate.boot.model.internal.TimeZoneStorageHelper.useColumnForTimeZoneStorage;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * @author Emmanuel Bernard
 */
public abstract class AbstractPropertyHolder implements PropertyHolder {
	private static final Logger log = CoreLogging.logger( AbstractPropertyHolder.class );

	private final String path;
	protected final AbstractPropertyHolder parent;
	private final MetadataBuildingContext context;

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
	public ConverterDescriptor resolveAttributeConverterDescriptor(MemberDetails attributeMember) {
		AttributeConversionInfo info = locateAttributeConversionInfo( attributeMember );
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

		log.debugf( "Attempting to locate auto-apply AttributeConverter for attributeMember [%s:%s]", path, attributeMember.getName() );

		return context.getMetadataCollector()
				.getConverterRegistry()
				.getAttributeConverterAutoApplyHandler()
				.findAutoApplyConverterForAttribute( attributeMember, context );
	}

	protected IllegalStateException buildExceptionFromInstantiationError(AttributeConversionInfo info, Exception e) {
		if ( void.class.equals( info.getConverterClass() ) ) {
			// the user forgot to set @Convert.converter
			// we already know it's not a @Convert.disableConversion
			return new IllegalStateException(
					"Unable to instantiate AttributeConverter: you left @Convert.converter to its default value void.",
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

	protected ConverterDescriptor makeAttributeConverterDescriptor(AttributeConversionInfo conversion) {
		try {
			return new ClassBasedConverterDescriptor(
					conversion.getConverterClass(),
					false,
					context.getBootstrapContext().getClassmateContext()
			);
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
		if ( parent != null ) {
			return parent.isInIdClass();
		}
		return false;
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

	protected SourceModelBuildingContext getSourceModelContext() {
		return getContext().getMetadataCollector().getSourceModelBuildingContext();
	}

	/**
	 * Set the property to be processed.  property can be null
	 *
	 * @param attributeMember The property
	 */
	protected void setCurrentProperty(MemberDetails attributeMember) {
		// todo (jpa32) : some of this (association override handling esp) does the same work multiple times - consolidate

		if ( attributeMember == null ) {
			this.currentPropertyColumnOverride = null;
			this.currentPropertyColumnTransformerOverride = null;
			this.currentPropertyJoinColumnOverride = null;
			this.currentPropertyJoinTableOverride = null;
			this.currentPropertyForeignKeyOverride = null;
		}
		else {
			this.currentPropertyColumnOverride = buildColumnOverride( attributeMember, getPath(), context );
			if ( this.currentPropertyColumnOverride.isEmpty() ) {
				this.currentPropertyColumnOverride = null;
			}

			this.currentPropertyColumnTransformerOverride = buildColumnTransformerOverride( attributeMember, context );
			if ( this.currentPropertyColumnTransformerOverride.isEmpty() ) {
				this.currentPropertyColumnTransformerOverride = null;
			}

			this.currentPropertyJoinColumnOverride = buildJoinColumnOverride( attributeMember, getPath(), context );
			if ( this.currentPropertyJoinColumnOverride.isEmpty() ) {
				this.currentPropertyJoinColumnOverride = null;
			}

			this.currentPropertyJoinTableOverride = buildJoinTableOverride( attributeMember, getPath(), context );
			if ( this.currentPropertyJoinTableOverride.isEmpty() ) {
				this.currentPropertyJoinTableOverride = null;
			}

			this.currentPropertyForeignKeyOverride = buildForeignKeyOverride( attributeMember, getPath(), context );
			if ( this.currentPropertyForeignKeyOverride.isEmpty() ) {
				this.currentPropertyForeignKeyOverride = null;
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
		final Column[] result = getExactOverriddenColumn( propertyName );
		if ( result == null ) {
			if ( propertyName.contains( ".{element}." ) ) {
				//support for non map collections where no prefix is needed
				//TODO cache the underlying regexp
				return getExactOverriddenColumn( propertyName.replace( ".{element}.", "."  ) );
			}
		}
		return result;
	}

	@Override
	public ColumnTransformer getOverriddenColumnTransformer(String logicalColumnName) {
		ColumnTransformer result = null;
		if ( parent != null ) {
			result = parent.getOverriddenColumnTransformer( logicalColumnName );
		}

		if ( result == null && currentPropertyColumnTransformerOverride != null ) {
			result = currentPropertyColumnTransformerOverride.get( logicalColumnName );
		}

		if ( result == null && holderColumnTransformerOverride != null ) {
			result = holderColumnTransformerOverride.get( logicalColumnName );
		}

		return result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * find the overridden rules from the exact property name.
	 */
	private Column[] getExactOverriddenColumn(String propertyName) {
		Column[] result = null;
		if ( parent != null ) {
			result = parent.getExactOverriddenColumn( propertyName );
		}

		if ( result == null && currentPropertyColumnOverride != null ) {
			result = currentPropertyColumnOverride.get( propertyName );
		}

		if ( result == null && holderColumnOverride != null ) {
			result = holderColumnOverride.get( propertyName );
		}

		return result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&amp;&amp;element' with nothing
	 * <p>
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 */
	@Override
	public JoinColumn[] getOverriddenJoinColumn(String propertyName) {
		final JoinColumn[] result = getExactOverriddenJoinColumn( propertyName );
		if ( result == null && propertyName.contains( ".{element}." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			return getExactOverriddenJoinColumn( propertyName.replace( ".{element}.", "."  ) );
		}
		return result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 */
	private JoinColumn[] getExactOverriddenJoinColumn(String propertyName) {
		JoinColumn[] result = null;
		if ( parent != null ) {
			result = parent.getExactOverriddenJoinColumn( propertyName );
		}

		if ( result == null && currentPropertyJoinColumnOverride != null ) {
			result = currentPropertyJoinColumnOverride.get( propertyName );
		}

		if ( result == null && holderJoinColumnOverride != null ) {
			result = holderJoinColumnOverride.get( propertyName );
		}

		return result;
	}

	@Override
	public ForeignKey getOverriddenForeignKey(String propertyName) {
		final ForeignKey result = getExactOverriddenForeignKey( propertyName );
		if ( result == null && propertyName.contains( ".{element}." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			return getExactOverriddenForeignKey( propertyName.replace( ".{element}.", "." ) );
		}
		return result;
	}

	private ForeignKey getExactOverriddenForeignKey(String propertyName) {
		ForeignKey result = null;
		if ( parent != null ) {
			result = parent.getExactOverriddenForeignKey( propertyName );
		}
		if ( result == null && currentPropertyForeignKeyOverride != null ) {
			result = currentPropertyForeignKeyOverride.get( propertyName );
		}
		if ( result == null && holderForeignKeyOverride != null ) {
			result = holderForeignKeyOverride.get( propertyName );
		}
		return result;
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
		final JoinTable result = getOverriddenJoinTable( propertyName );
		if ( result == null ) {
			return attributeMember.getDirectAnnotationUsage( JoinTable.class );
		}
		return result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&amp;&amp;element' with nothing
	 *
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 */
	public JoinTable getOverriddenJoinTable(String propertyName) {
		final JoinTable result = getExactOverriddenJoinTable( propertyName );
		if ( result == null && propertyName.contains( ".{element}." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			return getExactOverriddenJoinTable( propertyName.replace( ".{element}.", "."  ) );
		}
		return result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 */
	private JoinTable getExactOverriddenJoinTable(String propertyName) {
		JoinTable override = null;
		if ( parent != null ) {
			override = parent.getExactOverriddenJoinTable( propertyName );
		}
		if ( override == null && currentPropertyJoinTableOverride != null ) {
			override = currentPropertyJoinTableOverride.get( propertyName );
		}
		if ( override == null && holderJoinTableOverride != null ) {
			override = holderJoinTableOverride.get( propertyName );
		}
		return override;
	}

	private void buildHierarchyColumnOverride(ClassDetails element) {
		ClassDetails current = element;
		Map<String, Column[]> columnOverride = new HashMap<>();
		Map<String, ColumnTransformer> columnTransformerOverride = new HashMap<>();
		Map<String, JoinColumn[]> joinColumnOverride = new HashMap<>();
		Map<String, JoinTable> joinTableOverride = new HashMap<>();
		Map<String, ForeignKey> foreignKeyOverride = new HashMap<>();
		while ( current != null && !ClassDetails.OBJECT_CLASS_DETAILS.equals( current ) ) {
			if ( current.hasDirectAnnotationUsage( Entity.class )
					|| current.hasDirectAnnotationUsage( MappedSuperclass.class )
					|| current.hasDirectAnnotationUsage( Embeddable.class ) ) {
				//FIXME is embeddable override?
				Map<String, Column[]> currentOverride = buildColumnOverride( current, getPath(), context );
				Map<String, ColumnTransformer> currentTransformerOverride = buildColumnTransformerOverride( current, context );
				Map<String, JoinColumn[]> currentJoinOverride = buildJoinColumnOverride( current, getPath(), context );
				Map<String, JoinTable> currentJoinTableOverride = buildJoinTableOverride( current, getPath(), context );
				Map<String, ForeignKey> currentForeignKeyOverride = buildForeignKeyOverride( current, getPath(), context );
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

		holderColumnOverride = !columnOverride.isEmpty() ? columnOverride : null;
		holderColumnTransformerOverride = !columnTransformerOverride.isEmpty() ? columnTransformerOverride : null;
		holderJoinColumnOverride = !joinColumnOverride.isEmpty() ? joinColumnOverride : null;
		holderJoinTableOverride = !joinTableOverride.isEmpty() ? joinTableOverride : null;
		holderForeignKeyOverride = !foreignKeyOverride.isEmpty() ? foreignKeyOverride : null;
	}

	private static Map<String, Column[]> buildColumnOverride(
			AnnotationTarget element,
			String path,
			MetadataBuildingContext context) {
		final Map<String,Column[]> result = new HashMap<>();
		if ( element == null ) {
			return result;
		}

		final SourceModelBuildingContext sourceModelContext = context.getMetadataCollector().getSourceModelBuildingContext();
		final Map<String, List<Column>> columnOverrideMap = new HashMap<>();

		final AttributeOverride[] overrides = element.getRepeatedAnnotationUsages( AttributeOverride.class, sourceModelContext );
		if ( CollectionHelper.isNotEmpty( overrides ) ) {
			for ( AttributeOverride depAttr : overrides ) {
				final String qualifiedName = StringHelper.qualify( path, depAttr.name() );
				final Column column = depAttr.column();

				if ( columnOverrideMap.containsKey( qualifiedName ) ) {
					// already an entry, just add to that List
					columnOverrideMap.get( qualifiedName ).add( column );
				}
				else {
					// not yet an entry, create the list and add
					final List<Column> list = new ArrayList<>();
					list.add( column );
					columnOverrideMap.put( qualifiedName, list );
				}
			}
		}
		else if ( useColumnForTimeZoneStorage( element, context ) ) {
			final Column column = createTemporalColumn( element, path, context );
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
			final Column offsetColumn = createTimeZoneColumn( element, column, context );
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
		final TimeZoneColumn timeZoneColumn = element.getDirectAnnotationUsage( TimeZoneColumn.class );
		final ColumnJpaAnnotation created = JpaAnnotations.COLUMN.createUsage( context.getMetadataCollector().getSourceModelBuildingContext() );
		final String columnName = timeZoneColumn != null
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
		final Column annotatedColumn = element.getDirectAnnotationUsage( Column.class );
		if ( annotatedColumn != null ) {
			if ( StringHelper.isNotEmpty( annotatedColumn.name() ) ) {
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

		final ColumnJpaAnnotation created = JpaAnnotations.COLUMN.createUsage( context.getMetadataCollector().getSourceModelBuildingContext() );
		if ( StringHelper.isNotEmpty( implicitName.getText() ) ) {
			created.name( implicitName.getText() );
		}
		created.precision( precision );
		created.secondPrecision( secondPrecision );
		return created;
	}

	private static Map<String, ColumnTransformer> buildColumnTransformerOverride(AnnotationTarget element, MetadataBuildingContext context) {
		final SourceModelBuildingContext sourceModelContext = context.getMetadataCollector().getSourceModelBuildingContext();
		final Map<String, ColumnTransformer> columnOverride = new HashMap<>();
		if ( element != null ) {
			element.forEachAnnotationUsage( ColumnTransformer.class, sourceModelContext, (usage) -> {
				columnOverride.put( usage.forColumn(), usage );
			} );
		}
		return columnOverride;
	}

	private static Map<String, JoinColumn[]> buildJoinColumnOverride(AnnotationTarget element, String path, MetadataBuildingContext context) {
		final Map<String, JoinColumn[]> columnOverride = new HashMap<>();
		if ( element != null ) {
			final AssociationOverride[] overrides = buildAssociationOverrides( element, path, context );
			for ( AssociationOverride override : overrides ) {
				columnOverride.put(
						qualify( path, override.name() ),
						override.joinColumns()
				);
			}
		}
		return columnOverride;
	}

	private static Map<String, ForeignKey> buildForeignKeyOverride(AnnotationTarget element, String path, MetadataBuildingContext context) {
		final Map<String, ForeignKey> foreignKeyOverride = new HashMap<>();
		if ( element != null ) {
			final AssociationOverride[] overrides = buildAssociationOverrides( element, path, context );
			for ( AssociationOverride override : overrides ) {
				foreignKeyOverride.put(
						qualify( path, override.name() ),
						override.foreignKey()
				);
			}
		}
		return foreignKeyOverride;
	}

	private static AssociationOverride[] buildAssociationOverrides(AnnotationTarget element, String path, MetadataBuildingContext context) {
		return element.getRepeatedAnnotationUsages( AssociationOverride.class, context.getMetadataCollector().getSourceModelBuildingContext() );
	}

	private static Map<String, JoinTable> buildJoinTableOverride(AnnotationTarget element, String path, MetadataBuildingContext context) {
		final Map<String, JoinTable> result = new HashMap<>();
		if ( element != null ) {
			final AssociationOverride[] overrides = buildAssociationOverrides( element, path, context );
			for ( AssociationOverride override : overrides ) {
				final JoinColumn[] joinColumns = override.joinColumns();
				if ( CollectionHelper.isEmpty( joinColumns ) ) {
					result.put(
							qualify( path, override.name() ),
							override.joinTable()
					);
				}
			}
		}
		return result;
	}

	@Override
	public void setParentProperty(String parentProperty) {
		throw new AssertionFailure( "Setting the parent property to a non component" );
	}

	private static class ColumnImpl implements Column {

		private final String name;
		private final boolean unique;
		private final boolean nullable;
		private final boolean insertable;
		private final boolean updatable;
		private final String columnDefinition;
		private final String table;
		private final int precision;
		private final int secondPrecision;

		private ColumnImpl(
				String name,
				boolean unique,
				boolean nullable,
				boolean insertable,
				boolean updatable,
				String columnDefinition,
				String table,
				int precision,
				int secondPrecision) {
			this.name = name;
			this.unique = unique;
			this.nullable = nullable;
			this.insertable = insertable;
			this.updatable = updatable;
			this.columnDefinition = columnDefinition;
			this.table = table;
			this.precision = precision;
			this.secondPrecision = secondPrecision;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public boolean unique() {
			return unique;
		}

		@Override
		public boolean nullable() {
			return nullable;
		}

		@Override
		public boolean insertable() {
			return insertable;
		}

		@Override
		public boolean updatable() {
			return updatable;
		}

		@Override
		public String columnDefinition() {
			return columnDefinition;
		}

		@Override
		public String options() {
			return "";
		}

		@Override
		public String table() {
			return table;
		}

		@Override
		public int length() {
			return 255;
		}

		@Override
		public int precision() {
			return precision;
		}

		@Override
		public int scale() {
			return 0;
		}

		@Override
		public int secondPrecision() {
			return secondPrecision;
		}

		@Override
		public CheckConstraint[] check() {
			return new CheckConstraint[0];
		}

		@Override
		public String comment() {
			return "";
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return Column.class;
		}
	}
}
