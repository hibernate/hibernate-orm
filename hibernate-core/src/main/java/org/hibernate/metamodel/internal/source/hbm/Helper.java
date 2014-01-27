/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.TruthValue;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jaxb.spi.hbm.EntityElement;
import org.hibernate.jaxb.spi.hbm.JaxbCacheElement;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbJoinedSubclassElement;
import org.hibernate.jaxb.spi.hbm.JaxbLockModeAttribute;
import org.hibernate.jaxb.spi.hbm.JaxbNaturalIdCacheElement;
import org.hibernate.jaxb.spi.hbm.JaxbParamElement;
import org.hibernate.jaxb.spi.hbm.JaxbSqlDmlElement;
import org.hibernate.jaxb.spi.hbm.JaxbSubclassElement;
import org.hibernate.jaxb.spi.hbm.JaxbUnionSubclassElement;
import org.hibernate.jaxb.spi.hbm.TableInformationSource;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.binding.MetaAttribute;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.SizeSource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;

/**
 * A helper for dealing with
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class Helper {
	private static final String NATURAL_ID_CACHE_SUFFIX = "##NaturalId";
	public static final HibernateTypeSource TO_ONE_ATTRIBUTE_TYPE_SOURCE = new HibernateTypeSource() {
		@Override
		public String getName() {
			return null;
		}

		@Override
		public Map<String, String> getParameters() {
			return null;
		}
		@Override
		public Class getJavaType() {
			return null;
		}
	};

	public static LockMode interpretLockMode(JaxbLockModeAttribute lockModeAttribute, LocalBindingContext context){
		if ( lockModeAttribute == null ) {
			return LockMode.READ;
		}
		switch ( lockModeAttribute ) {
			case NONE:
				return LockMode.NONE;
			case UPGRADE:
				return LockMode.UPGRADE;
			case UPGRADE_NOWAIT:
				return LockMode.UPGRADE_NOWAIT;
			case READ:
				return LockMode.READ;
			case WRITE:
				return LockMode.WRITE;
			case FORCE:
				return LockMode.FORCE;
			case OPTIMISTIC:
				return LockMode.OPTIMISTIC;
			case OPTIMISTIC_FORCE_INCREMENT:
				return LockMode.OPTIMISTIC_FORCE_INCREMENT;
			case PESSIMISTIC_READ:
				return LockMode.PESSIMISTIC_READ;
			case PESSIMISTIC_WRITE:
				return LockMode.PESSIMISTIC_WRITE;
			case PESSIMISTIC_FORCE_INCREMENT:
				return LockMode.PESSIMISTIC_FORCE_INCREMENT;
			default:
				throw context.makeMappingException(  "unknown lock mode: "+lockModeAttribute );
		}
	}

	public static InheritanceType interpretInheritanceType(EntityElement entityElement) {
		if ( JaxbSubclassElement.class.isInstance( entityElement ) ) {
			return InheritanceType.SINGLE_TABLE;
		}
		else if ( JaxbJoinedSubclassElement.class.isInstance( entityElement ) ) {
			return InheritanceType.JOINED;
		}
		else if ( JaxbUnionSubclassElement.class.isInstance( entityElement ) ) {
			return InheritanceType.TABLE_PER_CLASS;
		}
		else {
			return InheritanceType.NO_INHERITANCE;
		}
	}

	/**
	 * Given a user-specified description of how to perform custom SQL, build the {@link CustomSQL} representation.
	 *
	 * @param customSqlElement User-specified description of how to perform custom SQL
	 *
	 * @return The {@link CustomSQL} representation
	 */
	public static CustomSQL buildCustomSql(JaxbSqlDmlElement customSqlElement) {
		if ( customSqlElement == null ) {
			return null;
		}
		final ExecuteUpdateResultCheckStyle checkStyle = customSqlElement.getCheck() == null
				? customSqlElement.isCallable()
						? ExecuteUpdateResultCheckStyle.NONE
						: ExecuteUpdateResultCheckStyle.COUNT
				: ExecuteUpdateResultCheckStyle.fromExternalName( customSqlElement.getCheck().value() );
		return new CustomSQL( customSqlElement.getValue(), customSqlElement.isCallable(), checkStyle );
	}

	/**
	 * Given the user-specified entity mapping, determine the appropriate entity name
	 *
	 * @param entityElement The user-specified entity mapping
	 * @param unqualifiedClassPackage The package to use for unqualified class names
	 *
	 * @return The appropriate entity name
	 */
	public static String determineEntityName(EntityElement entityElement, String unqualifiedClassPackage) {
		return entityElement.getEntityName() != null
				? entityElement.getEntityName()
				: qualifyIfNeeded( entityElement.getName(), unqualifiedClassPackage );
	}

	public static Caching createCaching(final JaxbCacheElement cacheElement, final String defaultRegionName) {
		if ( cacheElement == null ) {
			return null;
		}
		final String region = cacheElement.getRegion() != null ? cacheElement.getRegion() : defaultRegionName;
		final AccessType accessType = AccessType.fromExternalName( cacheElement.getUsage().value() );
		final boolean cacheLazyProps = !"non-lazy".equals( cacheElement.getInclude() );
		return new Caching( region, accessType, cacheLazyProps );
	}

	public static ValueHolder<Caching> createNaturalIdCachingHolder(final JaxbNaturalIdCacheElement cacheElement, final String entityName, final Caching entityCache) {
		return new ValueHolder<Caching>(
				new ValueHolder.DeferredInitializer<Caching>() {
					@Override
					public Caching initialize() {
						if ( cacheElement == null ) {
							return null;
						}
						final String region;
						if ( StringHelper.isEmpty( cacheElement.getRegion() ) ) {
							String temp = entityCache != null ? entityCache.getRegion() : entityName;
							region = temp + NATURAL_ID_CACHE_SUFFIX;
						}
						else {
							region = cacheElement.getRegion();
						}
						return new Caching( region, null, false );
					}
				}
		);
	}

	/**
	 * Qualify a (supposed class) name with the unqualified-class package name if it is not already qualified
	 *
	 * @param name The name
	 * @param unqualifiedClassPackage The unqualified-class package name
	 *
	 * @return {@code null} if the incoming name was {@code null}; or the qualified name.
	 */
	public static String qualifyIfNeeded(String name, String unqualifiedClassPackage) {
		if ( name == null ) {
			return null;
		}
		if ( name.indexOf( '.' ) < 0 && unqualifiedClassPackage != null ) {
			return unqualifiedClassPackage + '.' + name;
		}
		return name;
	}

	public static String getPropertyAccessorName(String access, boolean isEmbedded, String defaultAccess) {
		return getValue( access, isEmbedded ? "embedded" : defaultAccess );
	}

	public static MetaAttributeContext extractMetaAttributeContext(
			List<? extends MetaAttributeSource> metaAttributeSourceList,
			boolean onlyInheritable,
			MetaAttributeContext parentContext) {
		final MetaAttributeContext subContext = new MetaAttributeContext( parentContext );

		for ( MetaAttributeSource meta : metaAttributeSourceList ) {
			if ( onlyInheritable & !meta.isInheritable() ) {
				continue;
			}

			final String name = meta.getName();
			final MetaAttribute inheritedMetaAttribute = parentContext.getMetaAttribute( name );
			MetaAttribute metaAttribute = subContext.getLocalMetaAttribute( name );
			if ( metaAttribute == null || metaAttribute == inheritedMetaAttribute ) {
				metaAttribute = new MetaAttribute( name );
				subContext.add( metaAttribute );
			}
			metaAttribute.addValue( meta.getValue() );
		}

		return subContext;
	}

	public static <T> T getValue(T value, T defaultValue){
		return value == null ? defaultValue : value;
	}

	public static Set<CascadeStyle> interpretCascadeStyles(String cascades, LocalBindingContext bindingContext) {
		final Set<CascadeStyle> cascadeStyles = new HashSet<CascadeStyle>();
		if ( StringHelper.isEmpty( cascades ) ) {
			cascades = bindingContext.getMappingDefaults().getCascadeStyle();
		}
		for ( String cascade : StringHelper.split( " ,", cascades ) ) {
			cascadeStyles.add( CascadeStyles.getCascadeStyle( cascade ) );
		}
		return cascadeStyles;
	}

	public static Map<String, String> extractParameters(List<JaxbParamElement> xmlParamElements) {
		if ( xmlParamElements == null || xmlParamElements.isEmpty() ) {
			return Collections.emptyMap();
		}
		final HashMap<String,String> params = new HashMap<String, String>();
		for ( JaxbParamElement paramElement : xmlParamElements ) {
			params.put( paramElement.getName(), paramElement.getValue() );
		}
		return params;
	}

	public static SizeSource createSizeSourceIfMapped(Integer length, Integer precision, Integer scale) {
		if ( length != null || precision != null || scale != null ) {
			return new SizeSourceImpl( precision, scale, length );
		}
		return null;
	}

	public static Schema.Name determineDatabaseSchemaName(
			String explicitSchemaName,
			String explicitCatalogName,
			LocalBindingContext bindingContext) {
		return new Schema.Name(
				resolveIdentifier(
						explicitCatalogName,
						bindingContext.getMappingDefaults().getCatalogName(),
						bindingContext.isGloballyQuotedIdentifiers()
				), resolveIdentifier(
						explicitSchemaName,
						bindingContext.getMappingDefaults().getSchemaName(),
						bindingContext.isGloballyQuotedIdentifiers()
				)
		);
	}

	public static Identifier resolveIdentifier(String explicitName, String defaultName, boolean globalQuoting) {
		String name = StringHelper.isNotEmpty( explicitName ) ? explicitName : defaultName;
		if ( globalQuoting ) {
			name = StringHelper.quote( name );
		}
		return Identifier.toIdentifier( name );
	}

	/**
	 * Operates like SQL coalesce expression, except empty strings are treated as null.  Return the first non-empty value
	 *
	 * @param values The list of values.
	 * @param <T>
	 *
	 * @return The first non-empty value, or null if all values were empty
	 */
	public static <T> T coalesce(T... values) {
		if ( values == null ) {
			return null;
		}
		for ( T value : values ) {
			if ( value != null ) {
				if ( String.class.isInstance( value ) ) {
					if ( StringHelper.isNotEmpty( (String) value ) ) {
						return value;
					}
				}
				else {
					return value;
				}
			}
		}
		return null;
	}

	public static interface InLineViewNameInferrer {
		public String inferInLineViewName();
	}

	public static TableSpecificationSource createTableSource(
			MappingDocument mappingDocument,
			TableInformationSource jaxbTableSource,
			InLineViewNameInferrer inLineViewNameInferrer) {
		if ( jaxbTableSource.getSubselectAttribute() == null && jaxbTableSource.getSubselect() == null ) {
			return new TableSourceImpl(
					mappingDocument,
					jaxbTableSource.getSchema(),
					jaxbTableSource.getCatalog(),
					jaxbTableSource.getTable()
			);
		}
		else {
			return new InLineViewSourceImpl(
					mappingDocument,
					jaxbTableSource.getSchema(),
					jaxbTableSource.getCatalog(),
					jaxbTableSource.getSubselectAttribute() != null
							? jaxbTableSource.getSubselectAttribute()
							: jaxbTableSource.getSubselect(),
					jaxbTableSource.getTable() == null
							? inLineViewNameInferrer.inferInLineViewName()
							: jaxbTableSource.getTable()
			);
		}

	}

	/**
	 * For things that define one or more "value sources" there is a lot of variance in terms of how they
	 * look in the XML.  As an example, consider {@code <property/>} which might have:<ul>
	 *     <li>a {@code column} XML attribute</li>
	 *     <li>a {@code formula} XML attribute</li>
	 *     <li>one or more nested {@code <column/>} XML elements</li>
	 *     <li>a {@code <formula/>} XML element</li>
	 * </ul>
	 * as opposed to {@code <timestamp/>} which can only have:<ul>
	 *     <li>a {@code column} XML attribute</li>
	 * </ul>
	 * <p/>
	 * This class adapts those variances to a unified contract which is intended to be used in conjunction with
	 * {@link #buildValueSources}.
	 */
    public static class ValueSourcesAdapter {
        public String getContainingTableName() {
            return null;
        }

        public boolean isIncludedInInsertByDefault() {
            return false;
        }

        public boolean isIncludedInUpdateByDefault() {
            return false;
        }

        public String getColumnAttribute() {
            return null;
        }

        public String getFormulaAttribute() {
            return null;
        }

		public SizeSource getSizeSource() {
			return null;
		}

        public List<JaxbColumnElement> getColumn(){
			return Collections.emptyList();
		}

		public List<String> getFormula(){
			return Collections.emptyList();
		}

        public boolean isForceNotNull() {
            return false;
        }
    }

	private static void checkColumnOrFormulaElements(MappingDocument mappingDocument, ValueSourcesAdapter valueSourcesAdapter) {
		if ( CollectionHelper.isNotEmpty( valueSourcesAdapter.getColumn() ) || CollectionHelper.isNotEmpty(
				valueSourcesAdapter.getFormula()
		) ) {
			throw mappingDocument.getMappingLocalBindingContext().makeMappingException(
					"column/formula/size attribute may not be used together with <column>/<formula> subelement"
			);
		}
	}

	/**
	 * Given a {@link ValueSourcesAdapter}, build the corresponding list of {@link RelationalValueSource}
	 *
	 * @param mappingDocument the mapping document
	 * @param valueSourcesAdapter the adapter describing the value sources.
	 *
	 * @return The corresponding list.
	 */
    public static List<RelationalValueSource> buildValueSources(
			MappingDocument mappingDocument,
			ValueSourcesAdapter valueSourcesAdapter) {
		List<RelationalValueSource> result = new ArrayList<RelationalValueSource>();

		if ( StringHelper.isNotEmpty( valueSourcesAdapter.getColumnAttribute() ) ) {
			// we have the XML defining a column attribute.
			//		it is therefore illegal for there to also be any nested formula or column elements
			checkColumnOrFormulaElements(mappingDocument, valueSourcesAdapter);
			//		it is also illegal for there to also be a formula attribute
			if ( StringHelper.isNotEmpty( valueSourcesAdapter.getFormulaAttribute() ) ) {
				throw mappingDocument.getMappingLocalBindingContext().makeMappingException(
						"column and formula attributes may not be used together"
				);
			}

			result.add(
					new ColumnAttributeSourceImpl(
							mappingDocument,
							valueSourcesAdapter.getContainingTableName(),
							valueSourcesAdapter.getColumnAttribute(),
							valueSourcesAdapter.getSizeSource(),
							valueSourcesAdapter.isIncludedInInsertByDefault() ? TruthValue.TRUE : TruthValue.FALSE,
							valueSourcesAdapter.isIncludedInUpdateByDefault() ? TruthValue.TRUE : TruthValue.FALSE,
                            valueSourcesAdapter.isForceNotNull() ? TruthValue.FALSE : TruthValue.UNKNOWN
					)
			);
		}
		else if ( StringHelper.isNotEmpty( valueSourcesAdapter.getFormulaAttribute() ) ) {
			// we have the XML defining a formula attribute (and not a column attribute)
			//		it is therefore illegal for there to also be any nested formula or column elements
			checkColumnOrFormulaElements( mappingDocument, valueSourcesAdapter );
			// 		column/formula attribute combo checked already

			result.add(
					new FormulaImpl(
							mappingDocument,
							valueSourcesAdapter.getContainingTableName(),
							valueSourcesAdapter.getFormulaAttribute()
					)
			);
		}
		else if ( valueSourcesAdapter.getSizeSource() != null ) {
			// we have XML defining a length, precision, and/or scale attribute with neither
			// a column nor formula attribute; assume this is a column.
			//		it is therefore illegal for there to also be any nested formula or column elements
			checkColumnOrFormulaElements(mappingDocument, valueSourcesAdapter);
			result.add(
					new ColumnAttributeSourceImpl(
							mappingDocument,
							valueSourcesAdapter.getContainingTableName(),
							valueSourcesAdapter.getColumnAttribute(),
							valueSourcesAdapter.getSizeSource(),
							valueSourcesAdapter.isIncludedInInsertByDefault() ? TruthValue.TRUE : TruthValue.FALSE,
							valueSourcesAdapter.isIncludedInUpdateByDefault() ? TruthValue.TRUE : TruthValue.FALSE,
							valueSourcesAdapter.isForceNotNull() ? TruthValue.FALSE : TruthValue.UNKNOWN
					)
			);

		}
		// we have the XML defining nested formula or column elements (and not column attribute nor formula attribute)
		if ( CollectionHelper.isNotEmpty( valueSourcesAdapter.getColumn() ) ) {
			for ( JaxbColumnElement column : valueSourcesAdapter.getColumn() ) {
				result.add(
						new ColumnSourceImpl(
								mappingDocument,
								valueSourcesAdapter.getContainingTableName(),
								column,
								valueSourcesAdapter.isIncludedInInsertByDefault() ? TruthValue.TRUE : TruthValue.FALSE,
								valueSourcesAdapter.isIncludedInUpdateByDefault() ? TruthValue.TRUE : TruthValue.FALSE,
								valueSourcesAdapter.isForceNotNull() ? TruthValue.FALSE : TruthValue.TRUE
						)
				);
			}
		}
		if ( CollectionHelper.isNotEmpty( valueSourcesAdapter.getFormula() ) ) {
			for ( String formula : valueSourcesAdapter.getFormula() ) {
				result.add(
						new FormulaImpl(
								mappingDocument,
								valueSourcesAdapter.getContainingTableName(),
								formula
						)
				);
			}
		}
		return result;
	}
}
