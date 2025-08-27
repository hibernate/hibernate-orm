/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.boot.model.source.spi.Caching;
import org.hibernate.boot.model.CustomSql;
import org.hibernate.boot.model.source.spi.InheritanceType;
import org.hibernate.boot.model.source.spi.SizeSource;
import org.hibernate.boot.model.source.spi.TableSpecificationSource;
import org.hibernate.boot.model.source.spi.ToolingHint;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;

import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.EMBEDDED;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class Helper {
	public static InheritanceType interpretInheritanceType(JaxbHbmEntityBaseDefinition entityElement) {
		if ( entityElement instanceof JaxbHbmDiscriminatorSubclassEntityType ) {
			return InheritanceType.DISCRIMINATED;
		}
		else if ( entityElement instanceof JaxbHbmJoinedSubclassEntityType ) {
			return InheritanceType.JOINED;
		}
		else if ( entityElement instanceof JaxbHbmUnionSubclassEntityType ) {
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
		return cacheElement == null
				? new Caching()
				: new Caching(
						cacheElement.getRegion(),
						cacheElement.getUsage(),
						cacheElement.getInclude() == null
								|| !"non-lazy".equals( cacheElement.getInclude().value() ),
						true
				);
	}

	public static Caching createNaturalIdCaching(JaxbHbmNaturalIdCacheType cacheElement) {
		return cacheElement == null
				? new Caching()
				: new Caching(
						nullIfEmpty( cacheElement.getRegion() ),
						null,
						false,
						true
				);
	}

	public static String getPropertyAccessorName(String access, boolean isEmbedded, String defaultAccess) {
		return getValue( access, isEmbedded ? EMBEDDED.getExternalName() : defaultAccess );
	}

	public static <T> T getValue(T value, T defaultValue){
		return value == null ? defaultValue : value;
	}

	public static Map<String, String> extractParameters(List<JaxbHbmConfigParameterType> xmlParamElements) {
		if ( xmlParamElements == null || xmlParamElements.isEmpty() ) {
			return Collections.emptyMap();
		}
		final HashMap<String,String> params = new HashMap<>();
		for ( JaxbHbmConfigParameterType paramElement : xmlParamElements ) {
			params.put( paramElement.getName(), paramElement.getValue() );
		}
		return params;
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

	public interface InLineViewNameInferrer {
		String inferInLineViewName();
	}

	public static TableSpecificationSource createTableSource(
			MappingDocument mappingDocument,
			TableInformationContainer tableInformationContainer,
			InLineViewNameInferrer inLineViewNameInferrer,
			String rowId,
			String comment,
			String checkConstraint) {
		if ( StringHelper.isEmpty( tableInformationContainer.getSubselect() ) ) {
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
					tableInformationContainer.getSubselect(),
					tableInformationContainer.getTable() == null
							? inLineViewNameInferrer.inferInLineViewName()
							: tableInformationContainer.getTable(),
					comment
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
		final Class attributeOwnerClass = buildingContext.getBootstrapContext().getClassLoaderAccess().classForName( attributeOwnerClassName );
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
		return ReflectHelper.reflectedPropertyClass( attributeOwnerClass, attributeName );
	}
}
