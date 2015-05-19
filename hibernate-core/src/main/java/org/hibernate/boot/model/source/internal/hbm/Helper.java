/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCacheType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmConfigParameterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCustomSqlDmlType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNaturalIdCacheType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmToolingHintType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmUnionSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.TableInformationContainer;
import org.hibernate.boot.jaxb.hbm.spi.ToolingHintContainer;
import org.hibernate.boot.model.Caching;
import org.hibernate.boot.model.CustomSql;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.source.spi.InheritanceType;
import org.hibernate.boot.model.source.spi.SizeSource;
import org.hibernate.boot.model.source.spi.TableSpecificationSource;
import org.hibernate.boot.model.source.spi.ToolingHint;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class Helper {
	public static InheritanceType interpretInheritanceType(JaxbHbmEntityBaseDefinition entityElement) {
		if ( JaxbHbmDiscriminatorSubclassEntityType.class.isInstance( entityElement ) ) {
			return InheritanceType.DISCRIMINATED;
		}
		else if ( JaxbHbmJoinedSubclassEntityType.class.isInstance( entityElement ) ) {
			return InheritanceType.JOINED;
		}
		else if ( JaxbHbmUnionSubclassEntityType.class.isInstance( entityElement ) ) {
			return InheritanceType.UNION;
		}
		else {
			return InheritanceType.NO_INHERITANCE;
		}
	}

	/**
	 * Given a user-specified description of how to perform custom SQL, build the {@link CustomSql} representation.
	 *
	 * @param customSqlElement User-specified description of how to perform custom SQL
	 *
	 * @return The {@link CustomSql} representation
	 */
	public static CustomSql buildCustomSql(JaxbHbmCustomSqlDmlType customSqlElement) {
		if ( customSqlElement == null ) {
			return null;
		}
		final ExecuteUpdateResultCheckStyle checkStyle = customSqlElement.getCheck() == null
				? customSqlElement.isCallable()
						? ExecuteUpdateResultCheckStyle.NONE
						: ExecuteUpdateResultCheckStyle.COUNT
				: customSqlElement.getCheck();
		return new CustomSql( customSqlElement.getValue(), customSqlElement.isCallable(), checkStyle );
	}

	public static Caching createCaching(JaxbHbmCacheType cacheElement) {
		if ( cacheElement == null ) {
			// I'd really rather this be UNKNOWN, but the annotation version resolves this to TRUE/FALSE
			return new Caching( TruthValue.FALSE );
		}

		final boolean cacheLazyProps = cacheElement.getInclude() == null
				|| !"non-lazy".equals( cacheElement.getInclude().value() );

		return new Caching(
				cacheElement.getRegion(),
				cacheElement.getUsage(),
				cacheLazyProps,
				TruthValue.TRUE
		);
	}

	public static Caching createNaturalIdCaching(JaxbHbmNaturalIdCacheType cacheElement) {
		if ( cacheElement == null ) {
			return new Caching( TruthValue.UNKNOWN );
		}

		return new Caching(
				StringHelper.nullIfEmpty( cacheElement.getRegion() ),
				null,
				false,
				TruthValue.TRUE
		);
	}

	public static String getPropertyAccessorName(String access, boolean isEmbedded, String defaultAccess) {
		return getValue( access, isEmbedded ? "embedded" : defaultAccess );
	}

	public static <T> T getValue(T value, T defaultValue){
		return value == null ? defaultValue : value;
	}

	public static Map<String, String> extractParameters(List<JaxbHbmConfigParameterType> xmlParamElements) {
		if ( xmlParamElements == null || xmlParamElements.isEmpty() ) {
			return Collections.emptyMap();
		}
		final HashMap<String,String> params = new HashMap<String, String>();
		for ( JaxbHbmConfigParameterType paramElement : xmlParamElements ) {
			params.put( paramElement.getName(), paramElement.getValue() );
		}
		return params;
	}

	/**
	 * Operates like SQL coalesce expression, except empty strings are treated as null.  Return the first non-empty value
	 *
	 * @param values The list of values.
	 * @param <T> Generic type of values to coalesce
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

	static ToolingHintContext collectToolingHints(
			ToolingHintContext baseline,
			ToolingHintContainer toolingHintContainer) {
		return collectToolingHints( baseline, toolingHintContainer, false );
	}

	private static ToolingHintContext collectToolingHints(
			ToolingHintContext baseline,
			ToolingHintContainer toolingHintContainer,
			boolean onlyInheritable) {
		final ToolingHintContext localToolingHints = new ToolingHintContext( baseline );

		if ( toolingHintContainer != null && toolingHintContainer.getToolingHints() != null ) {
			for ( JaxbHbmToolingHintType toolingHintJaxbBinding : toolingHintContainer.getToolingHints() ) {
				if ( onlyInheritable && !toolingHintJaxbBinding.isInheritable() ) {
					continue;
				}

				final String hintName = toolingHintJaxbBinding.getName();
				ToolingHint toolingHint = localToolingHints.getToolingHint( hintName );

				if ( toolingHint == null ) {
					toolingHint = new ToolingHint( hintName, toolingHintJaxbBinding.isInheritable() );
					localToolingHints.add( toolingHint );
				}
				else {
					if ( baseline != null ) {
						final ToolingHint inherited = baseline.getToolingHint( hintName );
						if ( toolingHint == inherited ) {
							// overriding inherited meta attribute. HBX-621 & HBX-793
							toolingHint = new ToolingHint( hintName, toolingHintJaxbBinding.isInheritable() );
							localToolingHints.add( toolingHint );
						}
					}
				}

				toolingHint.addValue( toolingHintJaxbBinding.getValue() );
			}
		}

		return localToolingHints;
	}

	public static TableSpecificationSource createTableSource(
			MappingDocument mappingDocument,
			TableInformationContainer entityElement,
			InLineViewNameInferrer inLineViewNameInferrer) {
		return createTableSource( mappingDocument, entityElement, inLineViewNameInferrer, null, null, null );
	}

	public static interface InLineViewNameInferrer {
		public String inferInLineViewName();
	}

	public static TableSpecificationSource createTableSource(
			MappingDocument mappingDocument,
			TableInformationContainer tableInformationContainer,
			InLineViewNameInferrer inLineViewNameInferrer,
			String rowId,
			String comment,
			String checkConstraint) {
		if ( StringHelper.isEmpty( tableInformationContainer.getSubselectAttribute() )
				&& StringHelper.isEmpty( tableInformationContainer.getSubselect() ) ) {
			return new TableSourceImpl(
					mappingDocument,
					tableInformationContainer.getSchema(),
					tableInformationContainer.getCatalog(),
					tableInformationContainer.getTable(),
					rowId,
					comment,
					checkConstraint
			);
		}
		else {
			return new InLineViewSourceImpl(
					mappingDocument,
					tableInformationContainer.getSchema(),
					tableInformationContainer.getCatalog(),
					tableInformationContainer.getSubselectAttribute() != null
							? tableInformationContainer.getSubselectAttribute()
							: tableInformationContainer.getSubselect(),
					tableInformationContainer.getTable() == null
							? inLineViewNameInferrer.inferInLineViewName()
							: tableInformationContainer.getTable()
			);
		}
	}

	public static SizeSource interpretSizeSource(Integer length, Integer scale, Integer precision) {
		if ( length != null || precision != null || scale != null ) {
			return new SizeSourceImpl( length, scale, precision );
		}
		return null;
	}

	public static SizeSource interpretSizeSource(Integer length, String scale, String precision) {
		return interpretSizeSource(
				length,
				scale == null ? null : Integer.parseInt( scale ),
				precision == null ? null : Integer.parseInt( precision )
		);
	}

	public static Class reflectedPropertyClass(
			MetadataBuildingContext buildingContext,
			String attributeOwnerClassName,
			String attributeName) {
		final Class attributeOwnerClass = buildingContext.getClassLoaderAccess().classForName( attributeOwnerClassName );
		return reflectedPropertyClass(
				buildingContext,
				attributeOwnerClass,
				attributeName
		);
	}

	public static Class reflectedPropertyClass(
			MetadataBuildingContext buildingContext,
			Class attributeOwnerClass,
			final String attributeName) {
//		return BeanInfoHelper.visitBeanInfo(
//				attributeOwnerClass,
//				new BeanInfoHelper.ReturningBeanInfoDelegate<Class>() {
//					@Override
//					public Class processBeanInfo(BeanInfo beanInfo) throws Exception {
//						for ( PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
//							if ( propertyDescriptor.getName().equals( attributeName ) ) {
//								return propertyDescriptor.getPropertyType();
//							}
//						}
//						return null;
//					}
//				}
//		);
		return ReflectHelper.reflectedPropertyClass( attributeOwnerClass, attributeName );
	}
}
