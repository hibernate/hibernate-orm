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
package org.hibernate.metamodel.binder.source.hbm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.metamodel.binder.source.MetaAttributeContext;
import org.hibernate.metamodel.binder.source.hbm.xml.mapping.EntityElement;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.MetaAttribute;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLMetaElement;

/**
 * @author Steve Ebersole
 */
public class HbmHelper {

	// todo : merge this and MappingHelper together

	public static boolean isCallable(Element e) {
		return isCallable( e, true );
	}

	public static boolean isCallable(Element element, boolean supportsCallable) {
		Attribute attrib = element.attribute( "callable" );
		if ( attrib != null && "true".equals( attrib.getValue() ) ) {
			if ( !supportsCallable ) {
				throw new MappingException( "callable attribute not supported yet!" );
			}
			return true;
		}
		return false;
	}

	public static ExecuteUpdateResultCheckStyle getResultCheckStyle(String check, boolean callable) {
		if ( check == null ) {
			// use COUNT as the default.  This mimics the old behavior, although
			// NONE might be a better option moving forward in the case of callable
			return ExecuteUpdateResultCheckStyle.COUNT;
		}
		return ExecuteUpdateResultCheckStyle.fromExternalName( check );
	}

	public static final Map<String, MetaAttribute> extractMetas(List<XMLMetaElement> meta, Map<String, MetaAttribute> baseline) {
		return extractMetas( meta, false, baseline );
	}

	public static final Map<String, MetaAttribute> extractMetas(List<XMLMetaElement> metaList, boolean onlyInheritable, Map<String, MetaAttribute> baseline) {
		Map<String, MetaAttribute> extractedMetas = new HashMap<String, MetaAttribute>();
		extractedMetas.putAll( baseline );
		for ( XMLMetaElement meta : metaList ) {
			boolean inheritable = meta.isInherit();
			if ( onlyInheritable & !inheritable ) {
				continue;
			}

			final String name = meta.getAttribute();
			final MetaAttribute inheritedMetaAttribute = baseline.get( name );
			MetaAttribute metaAttribute = extractedMetas.get( name );
			if ( metaAttribute == null || metaAttribute == inheritedMetaAttribute ) {
				metaAttribute = new MetaAttribute( name );
				extractedMetas.put( name, metaAttribute );
			}
			metaAttribute.addValue( meta.getValue() );
		}
		return extractedMetas;
	}

	public static String extractEntityName(XMLHibernateMapping.XMLClass entityClazz, String unqualifiedPackageName) {
		return extractEntityName( entityClazz.getEntityName(), entityClazz.getName(), unqualifiedPackageName );
	}

	public static String extractEntityName(String entityName, String entityClassName, String unqualifiedPackageName) {
		return entityName == null ? getClassName( entityClassName, unqualifiedPackageName ) : entityName;
	}

	public static String determineEntityName(EntityElement entityElement, String packageName) {
		return extractEntityName( entityElement.getEntityName(), entityElement.getName(), packageName );
	}

	public static String determineClassName(EntityElement entityElement, String packageName) {
		return getClassName( entityElement.getName(), packageName );
	}

	public static String getClassName(Attribute att, String unqualifiedPackageName) {
		if ( att == null ) {
			return null;
		}
		return getClassName( att.getValue(), unqualifiedPackageName );
	}

	public static String getClassName(String unqualifiedName, String unqualifiedPackageName) {
		if ( unqualifiedName == null ) {
			return null;
		}
		if ( unqualifiedName.indexOf( '.' ) < 0 && unqualifiedPackageName != null ) {
			return unqualifiedPackageName + '.' + unqualifiedName;
		}
		return unqualifiedName;
	}

	public static CustomSQL getCustomSql(String sql, boolean isCallable, String check) {
		return new CustomSQL( sql.trim(), isCallable, getResultCheckStyle( check, isCallable ) );
	}

	public static String getPropertyAccessorName(String access, boolean isEmbedded, String defaultAccess) {
		return MappingHelper.getStringValue(
				access,
				isEmbedded ? "embedded" : defaultAccess
		);
	}

	public static MetaAttributeContext extractMetaAttributeContext(
			List<XMLMetaElement> metaElementList,
			MetaAttributeContext parentContext) {
		return extractMetaAttributeContext( metaElementList, false, parentContext );
	}

	public static MetaAttributeContext extractMetaAttributeContext(
			List<XMLMetaElement> metaElementList,
			boolean onlyInheritable,
			MetaAttributeContext parentContext) {
		final MetaAttributeContext subContext = new MetaAttributeContext( parentContext );

		for ( XMLMetaElement metaElement : metaElementList ) {
			if ( onlyInheritable & !metaElement.isInherit() ) {
				continue;
			}

			final String name = metaElement.getAttribute();
			final MetaAttribute inheritedMetaAttribute = parentContext.getMetaAttribute( name );
			MetaAttribute metaAttribute = subContext.getLocalMetaAttribute( name );
			if ( metaAttribute == null || metaAttribute == inheritedMetaAttribute ) {
				metaAttribute = new MetaAttribute( name );
				subContext.add( metaAttribute );
			}
			metaAttribute.addValue( metaElement.getValue() );
		}

		return subContext;
	}
}
