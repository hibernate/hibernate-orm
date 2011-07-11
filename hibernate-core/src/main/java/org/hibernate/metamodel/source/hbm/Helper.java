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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.metamodel.source.MetaAttributeContext;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.CustomSqlElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.EntityElement;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.binding.MetaAttribute;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLJoinedSubclassElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLMetaElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSubclassElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLUnionSubclassElement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class Helper {
	public static InheritanceType interpretInheritanceType(EntityElement entityElement) {
		if ( XMLSubclassElement.class.isInstance( entityElement ) ) {
			return InheritanceType.SINGLE_TABLE;
		}
		else if ( XMLJoinedSubclassElement.class.isInstance( entityElement ) ) {
			return InheritanceType.JOINED;
		}
		else if ( XMLUnionSubclassElement.class.isInstance( entityElement ) ) {
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
	public static CustomSQL buildCustomSql(CustomSqlElement customSqlElement) {
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
		return getStringValue( access, isEmbedded ? "embedded" : defaultAccess );
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

	public static String getStringValue(String value, String defaultValue) {
		return value == null ? defaultValue : value;
	}

	public static int getIntValue(String value, int defaultValue) {
		return value == null ? defaultValue : Integer.parseInt( value );
	}

	public static long getLongValue(String value, long defaultValue) {
		return value == null ? defaultValue : Long.parseLong( value );
	}

	public static boolean getBooleanValue(String value, boolean defaultValue) {
		return value == null ? defaultValue : Boolean.valueOf( value );
	}

	public static boolean getBooleanValue(Boolean value, boolean defaultValue) {
		return value == null ? defaultValue : value;
	}

	public static Set<String> getStringValueTokens(String str, String delimiters) {
		if ( str == null ) {
			return Collections.emptySet();
		}
		else {
			StringTokenizer tokenizer = new StringTokenizer( str, delimiters );
			Set<String> tokens = new HashSet<String>();
			while ( tokenizer.hasMoreTokens() ) {
				tokens.add( tokenizer.nextToken() );
			}
			return tokens;
		}
	}

	// todo : remove this once the state objects are cleaned up

	public static Class classForName(String className, ServiceRegistry serviceRegistry) {
		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		try {
			return classLoaderService.classForName( className );
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException( "Could not find class: " + className );
		}
	}
}
