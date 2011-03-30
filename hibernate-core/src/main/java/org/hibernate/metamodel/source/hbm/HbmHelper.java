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
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;

import org.hibernate.MappingException;
import org.hibernate.engine.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.source.util.DomHelper;

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

	public static ExecuteUpdateResultCheckStyle getResultCheckStyle(Element element, boolean callable) {
		Attribute attr = element.attribute( "check" );
		if ( attr == null ) {
			// use COUNT as the default.  This mimics the old behavior, although
			// NONE might be a better option moving forward in the case of callable
			return ExecuteUpdateResultCheckStyle.COUNT;
		}
		return ExecuteUpdateResultCheckStyle.parse( attr.getValue() );
	}

	public static final Map<String, MetaAttribute> extractMetas(Element node, Map<String, MetaAttribute> baseline) {
		return extractMetas( node, false, baseline );
	}

	public static final Map<String, MetaAttribute> extractMetas(Element node, boolean onlyInheritable, Map<String, MetaAttribute> baseline) {
		Map<String, MetaAttribute> extractedMetas = new HashMap<String, MetaAttribute>();
		extractedMetas.putAll( baseline );

		Iterator iter = node.elementIterator( "meta" );
		while ( iter.hasNext() ) {
			Element metaNode = (Element) iter.next();
			boolean inheritable = Boolean.valueOf( metaNode.attributeValue( "inherit" ) ).booleanValue();
			if ( onlyInheritable & !inheritable ) {
				continue;
			}

			final String name = metaNode.attributeValue( "attribute" );
			final MetaAttribute inheritedMetaAttribute = (MetaAttribute) baseline.get( name );
			MetaAttribute metaAttribute = (MetaAttribute) extractedMetas.get( name );
			if ( metaAttribute == null || metaAttribute == inheritedMetaAttribute ) {
				metaAttribute = new MetaAttribute( name );
				extractedMetas.put( name, metaAttribute );
			}
			metaAttribute.addValue( metaNode.getText() );
		}
		return extractedMetas;
	}

	public static String getSubselect(Element element) {
		String subselect = element.attributeValue( "subselect" );
		if ( subselect != null ) {
			return subselect;
		}
		else {
			Element subselectElement = element.element( "subselect" );
			return subselectElement == null ? null : subselectElement.getText();
		}
	}

	public static String extractEntityName(Element elem, String unqualifiedPackageName) {
		String entityName = elem.attributeValue( "entity-name" );
		return entityName == null ? getClassName( elem.attribute( "name" ), unqualifiedPackageName ) : entityName;
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

	public static CustomSQL getCustomSql(Element element )  {
		if ( element == null ) {
			return null; // EARLY EXIT!!!
		}
		boolean callable = DomHelper.extractBooleanAttributeValue( element, "callable", false );
		return new CustomSQL( element.getTextTrim(), callable, getResultCheckStyle( element, callable ) );
	}

	public static String getPropertyAccessorName(Element element, boolean isEmbedded, String defaultAccess) {
		return DomHelper.extractAttributeValue(
				element,
				"access",
				isEmbedded ? "embedded" : defaultAccess
		);
	}
}
