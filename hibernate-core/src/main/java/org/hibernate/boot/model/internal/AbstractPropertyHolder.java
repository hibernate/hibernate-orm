/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
import org.hibernate.annotations.ColumnTransformers;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.usertype.internal.AbstractTimeZoneStorageCompositeUserType;
import org.hibernate.usertype.internal.OffsetTimeCompositeUserType;

import org.jboss.logging.Logger;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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

	protected AbstractPropertyHolder parent;
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
	private final String path;
	private final MetadataBuildingContext context;
	private Boolean isInIdClass;

	AbstractPropertyHolder(
			String path,
			PropertyHolder parent,
			XClass clazzToProcess,
			MetadataBuildingContext context) {
		this.path = path;
		this.parent = (AbstractPropertyHolder) parent;
		this.context = context;
		buildHierarchyColumnOverride( clazzToProcess );
	}

	protected abstract String normalizeCompositePathForLogging(String attributeName);
	protected abstract String normalizeCompositePath(String attributeName);

	protected abstract AttributeConversionInfo locateAttributeConversionInfo(XProperty property);
	protected abstract AttributeConversionInfo locateAttributeConversionInfo(String path);

	@Override
	public ConverterDescriptor resolveAttributeConverterDescriptor(XProperty property) {
		AttributeConversionInfo info = locateAttributeConversionInfo( property );
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

		log.debugf( "Attempting to locate auto-apply AttributeConverter for property [%s:%s]", path, property.getName() );

		return context.getMetadataCollector()
				.getConverterRegistry()
				.getAttributeConverterAutoApplyHandler()
				.findAutoApplyConverterForAttribute( property, context );
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

	/**
	 * Set the property to be processed.  property can be null
	 *
	 * @param property The property
	 */
	protected void setCurrentProperty(XProperty property) {
		if ( property == null ) {
			this.currentPropertyColumnOverride = null;
			this.currentPropertyColumnTransformerOverride = null;
			this.currentPropertyJoinColumnOverride = null;
			this.currentPropertyJoinTableOverride = null;
			this.currentPropertyForeignKeyOverride = null;
		}
		else {
			this.currentPropertyColumnOverride = buildColumnOverride( property, getPath(), context );
			if ( this.currentPropertyColumnOverride.isEmpty() ) {
				this.currentPropertyColumnOverride = null;
			}

			this.currentPropertyColumnTransformerOverride = buildColumnTransformerOverride( property );
			if ( this.currentPropertyColumnTransformerOverride.isEmpty() ) {
				this.currentPropertyColumnTransformerOverride = null;
			}

			this.currentPropertyJoinColumnOverride = buildJoinColumnOverride( property, getPath() );
			if ( this.currentPropertyJoinColumnOverride.isEmpty() ) {
				this.currentPropertyJoinColumnOverride = null;
			}

			this.currentPropertyJoinTableOverride = buildJoinTableOverride( property, getPath() );
			if ( this.currentPropertyJoinTableOverride.isEmpty() ) {
				this.currentPropertyJoinTableOverride = null;
			}

			this.currentPropertyForeignKeyOverride = buildForeignKeyOverride( property, getPath() );
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
		Column[] result = getExactOverriddenColumn( propertyName );
		if ( result == null && propertyName.contains(".collection&&element.") ) {
			//support for non map collections where no prefix is needed
			result = getExactOverriddenColumn( propertyName.replace(".collection&&element.", ".") );
		}
		return result;
	}

	@Override
	public ColumnTransformer getOverriddenColumnTransformer(String logicalColumnName) {
		ColumnTransformer override = null;
		if ( parent != null ) {
			override = parent.getOverriddenColumnTransformer( logicalColumnName );
		}
		if ( override == null && currentPropertyColumnTransformerOverride != null ) {
			override = currentPropertyColumnTransformerOverride.get( logicalColumnName );
		}
		if ( override == null && holderColumnTransformerOverride != null ) {
			override = holderColumnTransformerOverride.get( logicalColumnName );
		}
		return override;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * find the overridden rules from the exact property name.
	 */
	private Column[] getExactOverriddenColumn(String propertyName) {
		Column[] override = null;
		if ( parent != null ) {
			override = parent.getExactOverriddenColumn( propertyName );
		}
		if ( override == null && currentPropertyColumnOverride != null ) {
			override = currentPropertyColumnOverride.get( propertyName );
		}
		if ( override == null && holderColumnOverride != null ) {
			override = holderColumnOverride.get( propertyName );
		}
		return override;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&amp;&amp;element' with nothing
	 * <p>
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 */
	@Override
	public JoinColumn[] getOverriddenJoinColumn(String propertyName) {
		JoinColumn[] result = getExactOverriddenJoinColumn( propertyName );
		if ( result == null && propertyName.contains( ".collection&&element." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			result = getExactOverriddenJoinColumn( propertyName.replace( ".collection&&element.", "."  ) );
		}
		return result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 */
	private JoinColumn[] getExactOverriddenJoinColumn(String propertyName) {
		JoinColumn[] override = null;
		if ( parent != null ) {
			override = parent.getExactOverriddenJoinColumn( propertyName );
		}
		if ( override == null && currentPropertyJoinColumnOverride != null ) {
			override = currentPropertyJoinColumnOverride.get( propertyName );
		}
		if ( override == null && holderJoinColumnOverride != null ) {
			override = holderJoinColumnOverride.get( propertyName );
		}
		return override;
	}

	@Override
	public ForeignKey getOverriddenForeignKey(String propertyName) {
		ForeignKey result = getExactOverriddenForeignKey( propertyName );
		if ( result == null && propertyName.contains( ".collection&&element." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			result = getExactOverriddenForeignKey( propertyName.replace( ".collection&&element.", "." ) );
		}
		return result;
	}

	private ForeignKey getExactOverriddenForeignKey(String propertyName) {
		ForeignKey override = null;
		if ( parent != null ) {
			override = parent.getExactOverriddenForeignKey( propertyName );
		}
		if ( override == null && currentPropertyForeignKeyOverride != null ) {
			override = currentPropertyForeignKeyOverride.get( propertyName );
		}
		if ( override == null && holderForeignKeyOverride != null ) {
			override = holderForeignKeyOverride.get( propertyName );
		}
		return override;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&amp;&amp;element' with nothing
	 *
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 */
	@Override
	public JoinTable getJoinTable(XProperty property) {
		final String propertyName = qualify( getPath(), property.getName() );
		JoinTable result = getOverriddenJoinTable( propertyName );
		if (result == null) {
			result = property.getAnnotation( JoinTable.class );
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
		JoinTable result = getExactOverriddenJoinTable( propertyName );
		if ( result == null && propertyName.contains( ".collection&&element." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			result = getExactOverriddenJoinTable( propertyName.replace( ".collection&&element.", "."  ) );
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

	private void buildHierarchyColumnOverride(XClass element) {
		XClass current = element;
		Map<String, Column[]> columnOverride = new HashMap<>();
		Map<String, ColumnTransformer> columnTransformerOverride = new HashMap<>();
		Map<String, JoinColumn[]> joinColumnOverride = new HashMap<>();
		Map<String, JoinTable> joinTableOverride = new HashMap<>();
		Map<String, ForeignKey> foreignKeyOverride = new HashMap<>();
		XClass objectClass = context.getBootstrapContext().getReflectionManager().toXClass(Object.class);
		while ( current != null && !objectClass.equals( current ) ) {
			if ( current.isAnnotationPresent( Entity.class )
					|| current.isAnnotationPresent( MappedSuperclass.class )
					|| current.isAnnotationPresent( Embeddable.class ) ) {
				//FIXME is embeddable override?
				Map<String, Column[]> currentOverride = buildColumnOverride( current, getPath(), context );
				Map<String, ColumnTransformer> currentTransformerOverride = buildColumnTransformerOverride( current );
				Map<String, JoinColumn[]> currentJoinOverride = buildJoinColumnOverride( current, getPath() );
				Map<String, JoinTable> currentJoinTableOverride = buildJoinTableOverride( current, getPath() );
				Map<String, ForeignKey> currentForeignKeyOverride = buildForeignKeyOverride( current, getPath() );
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
			current = current.getSuperclass();
		}

		holderColumnOverride = !columnOverride.isEmpty() ? columnOverride : null;
		holderColumnTransformerOverride = !columnTransformerOverride.isEmpty() ? columnTransformerOverride : null;
		holderJoinColumnOverride = !joinColumnOverride.isEmpty() ? joinColumnOverride : null;
		holderJoinTableOverride = !joinTableOverride.isEmpty() ? joinTableOverride : null;
		holderForeignKeyOverride = !foreignKeyOverride.isEmpty() ? foreignKeyOverride : null;
	}

	private static Map<String, Column[]> buildColumnOverride(
			XAnnotatedElement element,
			String path,
			MetadataBuildingContext context) {
		final Map<String, Column[]> columnOverride = new HashMap<>();
		if ( element != null ) {
			AttributeOverride singleOverride = element.getAnnotation( AttributeOverride.class );
			AttributeOverrides multipleOverrides = element.getAnnotation( AttributeOverrides.class );
			AttributeOverride[] overrides;
			if ( singleOverride != null ) {
				overrides = new AttributeOverride[]{ singleOverride };
			}
			else if ( multipleOverrides != null ) {
				overrides = multipleOverrides.value();
			}
			else {
				overrides = null;
			}

			if ( overrides != null ) {
				final Map<String, List<Column>> columnOverrideList = new HashMap<>();

				for ( AttributeOverride depAttr : overrides ) {
					final String qualifiedName = qualify( path, depAttr.name() );

					if ( columnOverrideList.containsKey( qualifiedName ) ) {
						columnOverrideList.get( qualifiedName ).add( depAttr.column() );
					}
					else {
						List<Column> list = new ArrayList<>();
						list.add( depAttr.column() );
						columnOverrideList.put( qualifiedName, list );
					}
				}

				for ( Map.Entry<String, List<Column>> entry : columnOverrideList.entrySet() ) {
					columnOverride.put( entry.getKey(), entry.getValue().toArray( new Column[0] ) );
				}
			}
			else if ( useColumnForTimeZoneStorage( element, context ) ) {
				final Column column = createTemporalColumn( element, path, context );
				if ( isOffsetTimeClass( element ) ) {
					columnOverride.put(
							path + "." + OffsetTimeCompositeUserType.LOCAL_TIME_NAME,
							new Column[] { column }
					);
				}
				else {
					columnOverride.put(
							path + "." + AbstractTimeZoneStorageCompositeUserType.INSTANT_NAME,
							new Column[] { column }
					);
				}
				final Column offsetColumn = createTimeZoneColumn( element, column );
				columnOverride.put(
						path + "." + AbstractTimeZoneStorageCompositeUserType.ZONE_OFFSET_NAME,
						new Column[]{ offsetColumn }
				);
			}
		}
		return columnOverride;
	}

	private static Column createTimeZoneColumn(XAnnotatedElement element, Column column) {
		final TimeZoneColumn timeZoneColumn = element.getAnnotation( TimeZoneColumn.class );
		if ( timeZoneColumn != null ) {
			return new ColumnImpl(
				timeZoneColumn.name(),
				false,
				column.nullable(),
				timeZoneColumn.insertable(),
				timeZoneColumn.updatable(),
				timeZoneColumn.columnDefinition(),
				timeZoneColumn.table(),
				0
			);
		}
		else {
			return new ColumnImpl(
					column.name() + "_tz",
					false,
					column.nullable(),
					column.insertable(),
					column.updatable(),
					"",
					column.table(),
					0
			);
		}
	}

	private static Column createTemporalColumn(XAnnotatedElement element, String path, MetadataBuildingContext context) {
		int precision;
		final Column annotatedColumn = element.getAnnotation( Column.class );
		if ( annotatedColumn != null ) {
			if ( !annotatedColumn.name().isEmpty() ) {
				return annotatedColumn;
			}
			precision = annotatedColumn.precision();
		}
		else {
			precision = 0;
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
		return new ColumnImpl(
				implicitName.getText(),
				false,
				true,
				true,
				true,
				"",
				"",
				precision
		);
	}

	private static Map<String, ColumnTransformer> buildColumnTransformerOverride(XAnnotatedElement element) {
		Map<String, ColumnTransformer> columnOverride = new HashMap<>();
		if ( element != null ) {
			ColumnTransformer singleOverride = element.getAnnotation( ColumnTransformer.class );
			ColumnTransformers multipleOverrides = element.getAnnotation( ColumnTransformers.class );
			ColumnTransformer[] overrides;
			if ( singleOverride != null ) {
				overrides = new ColumnTransformer[]{ singleOverride };
			}
			else if ( multipleOverrides != null ) {
				overrides = multipleOverrides.value();
			}
			else {
				overrides = null;
			}

			if ( overrides != null ) {
				for ( ColumnTransformer depAttr : overrides ) {
					columnOverride.put(
							depAttr.forColumn(),
							depAttr
					);
				}
			}
		}
		return columnOverride;
	}

	private static Map<String, JoinColumn[]> buildJoinColumnOverride(XAnnotatedElement element, String path) {
		Map<String, JoinColumn[]> columnOverride = new HashMap<>();
		if ( element != null ) {
			AssociationOverride[] overrides = buildAssociationOverrides( element, path );
			if ( overrides != null ) {
				for ( AssociationOverride depAttr : overrides ) {
					columnOverride.put(
							qualify( path, depAttr.name() ),
							depAttr.joinColumns()
					);
				}
			}
		}
		return columnOverride;
	}

	private static Map<String, ForeignKey> buildForeignKeyOverride(XAnnotatedElement element, String path) {
		Map<String, ForeignKey> foreignKeyOverride = new HashMap<>();
		if ( element != null ) {
			AssociationOverride[] overrides = buildAssociationOverrides( element, path );
			if ( overrides != null ) {
				for ( AssociationOverride depAttr : overrides ) {
					foreignKeyOverride.put( qualify( path, depAttr.name() ), depAttr.foreignKey() );
				}
			}
		}
		return foreignKeyOverride;
	}

	private static AssociationOverride[] buildAssociationOverrides(XAnnotatedElement element, String path) {
		AssociationOverride singleOverride = element.getAnnotation( AssociationOverride.class );
		AssociationOverrides pluralOverrides = element.getAnnotation( AssociationOverrides.class );

		AssociationOverride[] overrides;
		if ( singleOverride != null ) {
			overrides = new AssociationOverride[] { singleOverride };
		}
		else if ( pluralOverrides != null ) {
			overrides = pluralOverrides.value();
		}
		else {
			overrides = null;
		}
		return overrides;
	}

	private static Map<String, JoinTable> buildJoinTableOverride(XAnnotatedElement element, String path) {
		Map<String, JoinTable> tableOverride = new HashMap<>();
		if ( element != null ) {
			AssociationOverride[] overrides = buildAssociationOverrides( element, path );
			if ( overrides != null ) {
				for ( AssociationOverride depAttr : overrides ) {
					if ( depAttr.joinColumns().length == 0 ) {
						tableOverride.put(
								qualify( path, depAttr.name() ),
								depAttr.joinTable()
						);
					}
				}
			}
		}
		return tableOverride;
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

		private ColumnImpl(
				String name,
				boolean unique,
				boolean nullable,
				boolean insertable,
				boolean updatable,
				String columnDefinition,
				String table,
				int precision) {
			this.name = name;
			this.unique = unique;
			this.nullable = nullable;
			this.insertable = insertable;
			this.updatable = updatable;
			this.columnDefinition = columnDefinition;
			this.table = table;
			this.precision = precision;
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
		public Class<? extends Annotation> annotationType() {
			return Column.class;
		}
	}
}
