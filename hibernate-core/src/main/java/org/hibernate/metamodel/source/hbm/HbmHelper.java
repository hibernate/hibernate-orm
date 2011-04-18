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
package org.hibernate.metamodel.source.hbm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;

import org.hibernate.MappingException;
import org.hibernate.engine.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.util.DomHelper;
import org.hibernate.metamodel.source.util.MappingHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class HbmHelper {
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
		return ExecuteUpdateResultCheckStyle.parse( check );
	}

	public static final Map<String, MetaAttribute> extractMetas(List<org.hibernate.metamodel.source.hbm.xml.mapping.Meta> meta, Map<String, MetaAttribute> baseline) {
		return extractMetas( meta, false, baseline );
	}

	public static final Map<String, MetaAttribute> extractMetas(List<org.hibernate.metamodel.source.hbm.xml.mapping.Meta> metaList, boolean onlyInheritable, Map<String, MetaAttribute> baseline) {
		Map<String, MetaAttribute> extractedMetas = new HashMap<String, MetaAttribute>();
		extractedMetas.putAll( baseline );
		for ( org.hibernate.metamodel.source.hbm.xml.mapping.Meta meta : metaList) {
			boolean inheritable = Boolean.valueOf( meta.getInherit() );
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
			metaAttribute.addValue( meta.getContent() );
		}
		return extractedMetas;
	}

	public static String extractEntityName( org.hibernate.metamodel.source.hbm.xml.mapping.Class entityClazz, String unqualifiedPackageName) {
		String entityName = entityClazz.getEntityName();
		return entityName == null ? getClassName( entityClazz.getName(), unqualifiedPackageName ) : entityName;
	}

	public static String getClassName(Attribute att, String unqualifiedPackageName) {
		if ( att == null ) return null;
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

	public static CustomSQL getCustomSql(String sql, boolean isCallable, String check )  {
		return new CustomSQL( sql.trim(), isCallable, getResultCheckStyle( check, isCallable ) );
	}

	public static String getPropertyAccessorName(Element element, boolean isEmbedded, String defaultAccess) {
		return DomHelper.extractAttributeValue(
				element,
				"access",
				isEmbedded ? "embedded" : defaultAccess
		);
	}

	public static String getPropertyAccessorName(String access, boolean isEmbedded, String defaultAccess) {
		return MappingHelper.getStringValue(
				access,
				isEmbedded ? "embedded" : defaultAccess
		);
	}
}
