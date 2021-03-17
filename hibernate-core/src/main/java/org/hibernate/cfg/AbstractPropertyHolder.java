/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MappedSuperclass;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

/**
 * No idea.
 *
 * @author Emmanuel Bernard
 */
public abstract class AbstractPropertyHolder implements PropertyHolder {
	private static final Logger log = CoreLogging.logger( AbstractPropertyHolder.class );

	protected AbstractPropertyHolder parent;
	private Map<String, Column[]> holderColumnOverride;
	private Map<String, Column[]> currentPropertyColumnOverride;
	private Map<String, JoinColumn[]> holderJoinColumnOverride;
	private Map<String, JoinColumn[]> currentPropertyJoinColumnOverride;
	private Map<String, JoinTable> holderJoinTableOverride;
	private Map<String, JoinTable> currentPropertyJoinTableOverride;
	private Map<String, ForeignKey> holderForeignKeyOverride;
	private Map<String, ForeignKey> currentPropertyForeignKeyOverride;
	private String path;
	private MetadataBuildingContext context;
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
		return isInIdClass != null ? isInIdClass : parent != null ? parent.isInIdClass() : false;
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
			this.currentPropertyJoinColumnOverride = null;
			this.currentPropertyJoinTableOverride = null;
			this.currentPropertyForeignKeyOverride = null;
		}
		else {
			this.currentPropertyColumnOverride = buildColumnOverride( property, getPath() );
			if ( this.currentPropertyColumnOverride.size() == 0 ) {
				this.currentPropertyColumnOverride = null;
			}

			this.currentPropertyJoinColumnOverride = buildJoinColumnOverride( property, getPath() );
			if ( this.currentPropertyJoinColumnOverride.size() == 0 ) {
				this.currentPropertyJoinColumnOverride = null;
			}

			this.currentPropertyJoinTableOverride = buildJoinTableOverride( property, getPath() );
			if ( this.currentPropertyJoinTableOverride.size() == 0 ) {
				this.currentPropertyJoinTableOverride = null;
			}

			this.currentPropertyForeignKeyOverride = buildForeignKeyOverride( property, getPath() );
			if ( this.currentPropertyForeignKeyOverride.size() == 0 ) {
				this.currentPropertyForeignKeyOverride = null;
			}
		}
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&&element' with nothing
	 *
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 */
	@Override
	public Column[] getOverriddenColumn(String propertyName) {
		Column[] result = getExactOverriddenColumn( propertyName );
		if (result == null) {
			//the commented code can be useful if people use the new prefixes on old mappings and vice versa
			// if we enable them:
			// WARNING: this can conflict with user's expectations if:
	 		//  - the property uses some restricted values
	 		//  - the user has overridden the column
			// also change getOverriddenJoinColumn and getOverriddenJoinTable as well

//			if ( propertyName.contains( ".key." ) ) {
//				//support for legacy @AttributeOverride declarations
//				//TODO cache the underlying regexp
//				result = getExactOverriddenColumn( propertyName.replace( ".key.", ".index."  ) );
//			}
//			if ( result == null && propertyName.endsWith( ".key" ) ) {
//				//support for legacy @AttributeOverride declarations
//				//TODO cache the underlying regexp
//				result = getExactOverriddenColumn(
//						propertyName.substring( 0, propertyName.length() - ".key".length() ) + ".index"
//						);
//			}
//			if ( result == null && propertyName.contains( ".value." ) ) {
//				//support for legacy @AttributeOverride declarations
//				//TODO cache the underlying regexp
//				result = getExactOverriddenColumn( propertyName.replace( ".value.", ".element."  ) );
//			}
//			if ( result == null && propertyName.endsWith( ".value" ) ) {
//				//support for legacy @AttributeOverride declarations
//				//TODO cache the underlying regexp
//				result = getExactOverriddenColumn(
//						propertyName.substring( 0, propertyName.length() - ".value".length() ) + ".element"
//						);
//			}
			if ( result == null && propertyName.contains( ".collection&&element." ) ) {
				//support for non map collections where no prefix is needed
				//TODO cache the underlying regexp
				result = getExactOverriddenColumn( propertyName.replace( ".collection&&element.", "."  ) );
			}
		}
		return result;
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
	 * replace the placeholder 'collection&&element' with nothing
	 *
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
	 * replace the placeholder 'collection&&element' with nothing
	 *
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 */
	@Override
	public JoinTable getJoinTable(XProperty property) {
		final String propertyName = StringHelper.qualify( getPath(), property.getName() );
		JoinTable result = getOverriddenJoinTable( propertyName );
		if (result == null) {
			result = property.getAnnotation( JoinTable.class );
		}
		return result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&&element' with nothing
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
		Map<String, Column[]> columnOverride = new HashMap<String, Column[]>();
		Map<String, JoinColumn[]> joinColumnOverride = new HashMap<String, JoinColumn[]>();
		Map<String, JoinTable> joinTableOverride = new HashMap<String, JoinTable>();
		Map<String, ForeignKey> foreignKeyOverride = new HashMap<String, ForeignKey>();
		while ( current != null && !context.getBootstrapContext().getReflectionManager().toXClass( Object.class ).equals( current ) ) {
			if ( current.isAnnotationPresent( Entity.class ) || current.isAnnotationPresent( MappedSuperclass.class )
					|| current.isAnnotationPresent( Embeddable.class ) ) {
				//FIXME is embeddable override?
				Map<String, Column[]> currentOverride = buildColumnOverride( current, getPath() );
				Map<String, JoinColumn[]> currentJoinOverride = buildJoinColumnOverride( current, getPath() );
				Map<String, JoinTable> currentJoinTableOverride = buildJoinTableOverride( current, getPath() );
				Map<String, ForeignKey> currentForeignKeyOverride = buildForeignKeyOverride( current, getPath() );
				currentOverride.putAll( columnOverride ); //subclasses have precedence over superclasses
				currentJoinOverride.putAll( joinColumnOverride ); //subclasses have precedence over superclasses
				currentJoinTableOverride.putAll( joinTableOverride ); //subclasses have precedence over superclasses
				currentForeignKeyOverride.putAll( foreignKeyOverride ); //subclasses have precedence over superclasses
				columnOverride = currentOverride;
				joinColumnOverride = currentJoinOverride;
				joinTableOverride = currentJoinTableOverride;
				foreignKeyOverride = currentForeignKeyOverride;
			}
			current = current.getSuperclass();
		}

		holderColumnOverride = columnOverride.size() > 0 ? columnOverride : null;
		holderJoinColumnOverride = joinColumnOverride.size() > 0 ? joinColumnOverride : null;
		holderJoinTableOverride = joinTableOverride.size() > 0 ? joinTableOverride : null;
		holderForeignKeyOverride = foreignKeyOverride.size() > 0 ? foreignKeyOverride : null;
	}

	private static Map<String, Column[]> buildColumnOverride(XAnnotatedElement element, String path) {
		Map<String, Column[]> columnOverride = new HashMap<String, Column[]>();
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
				Map<String, List<Column>> columnOverrideList = new HashMap<>();

				for ( AttributeOverride depAttr : overrides ) {
					String qualifiedName = StringHelper.qualify( path, depAttr.name() );

					if ( columnOverrideList.containsKey( qualifiedName ) ) {
						columnOverrideList.get( qualifiedName ).add( depAttr.column() );
					}
					else {
						columnOverrideList.put(
							qualifiedName,
							new ArrayList<>( Arrays.asList( depAttr.column() ) )
						);
					}
				}

				for (Map.Entry<String, List<Column>> entry : columnOverrideList.entrySet()) {
					columnOverride.put(
						entry.getKey(),
						entry.getValue().toArray( new Column[entry.getValue().size()] )
					);
				}
			}
		}
		return columnOverride;
	}

	private static Map<String, JoinColumn[]> buildJoinColumnOverride(XAnnotatedElement element, String path) {
		Map<String, JoinColumn[]> columnOverride = new HashMap<String, JoinColumn[]>();
		if ( element != null ) {
			AssociationOverride[] overrides = buildAssociationOverrides( element, path );
			if ( overrides != null ) {
				for ( AssociationOverride depAttr : overrides ) {
					columnOverride.put(
							StringHelper.qualify( path, depAttr.name() ),
							depAttr.joinColumns()
					);
				}
			}
		}
		return columnOverride;
	}

	private static Map<String, ForeignKey> buildForeignKeyOverride(XAnnotatedElement element, String path) {
		Map<String, ForeignKey> foreignKeyOverride = new HashMap<String, ForeignKey>();
		if ( element != null ) {
			AssociationOverride[] overrides = buildAssociationOverrides( element, path );
			if ( overrides != null ) {
				for ( AssociationOverride depAttr : overrides ) {
					foreignKeyOverride.put( StringHelper.qualify( path, depAttr.name() ), depAttr.foreignKey() );
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
		Map<String, JoinTable> tableOverride = new HashMap<String, JoinTable>();
		if ( element != null ) {
			AssociationOverride[] overrides = buildAssociationOverrides( element, path );
			if ( overrides != null ) {
				for ( AssociationOverride depAttr : overrides ) {
					if ( depAttr.joinColumns().length == 0 ) {
						tableOverride.put(
								StringHelper.qualify( path, depAttr.name() ),
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
}
