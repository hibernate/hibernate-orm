/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations.util;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.GenerationType;

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;

/**
 * Helper class which converts between different enum types.
 *
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
public class EnumConversionHelper {
	private EnumConversionHelper() {
	}

	public static String generationTypeToGeneratorStrategyName(GenerationType generatorEnum, boolean useNewGeneratorMappings) {
		switch ( generatorEnum ) {
			case IDENTITY:
				return "identity";
			case AUTO:
				return useNewGeneratorMappings
						? "enhanced-sequence"
						: "native";
			case TABLE:
				return useNewGeneratorMappings
						? "enhanced-table"
						: MultipleHiLoPerTableGenerator.class.getName();
			case SEQUENCE:
				return useNewGeneratorMappings
						? "enhanced-sequence"
						: "seqhilo";
		}
		throw new AssertionFailure( "Unknown GeneratorType: " + generatorEnum );
	}

	public static CascadeStyle cascadeTypeToCascadeStyle(CascadeType cascadeType) {
		switch ( cascadeType ) {
			case ALL: {
				return CascadeStyles.ALL;
			}
			case PERSIST: {
				return CascadeStyles.PERSIST;
			}
			case MERGE: {
				return CascadeStyles.MERGE;
			}
			case REMOVE: {
				return CascadeStyles.DELETE;
			}
			case REFRESH: {
				return CascadeStyles.REFRESH;
			}
			case DETACH: {
				return CascadeStyles.EVICT;
			}
			default: {
				throw new AssertionFailure( "Unknown cascade type" );
			}
		}
	}
	
	public static CascadeStyle cascadeTypeToCascadeStyle(
			org.hibernate.annotations.CascadeType cascadeType) {
		switch ( cascadeType ) {
			case ALL: {
				return CascadeStyles.ALL;
			}
			case PERSIST: {
				return CascadeStyles.PERSIST;
			}
			case MERGE: {
				return CascadeStyles.MERGE;
			}
			case REMOVE: {
				return CascadeStyles.DELETE;
			}
			case REFRESH: {
				return CascadeStyles.REFRESH;
			}
			case DETACH: {
				return CascadeStyles.EVICT;
			}
			case DELETE: {
				return CascadeStyles.DELETE;
			}
			case SAVE_UPDATE: {
				return CascadeStyles.UPDATE;
			}
			case REPLICATE: {
				return CascadeStyles.REPLICATE;
			}
			case LOCK: {
				return CascadeStyles.LOCK;
			}
			default: {
				throw new AssertionFailure( "Unknown cascade type: " + cascadeType );
			}
		}
	}

	public static FetchMode annotationFetchModeToHibernateFetchMode(org.hibernate.annotations.FetchMode annotationFetchMode) {
		switch ( annotationFetchMode ) {
			case JOIN: {
				return FetchMode.JOIN;
			}
			case SELECT: {
				return FetchMode.SELECT;
			}
			case SUBSELECT: {
				// todo - is this correct? can the conversion be made w/o any additional information, eg
				// todo - association nature
				return FetchMode.SELECT;
			}
			default: {
				throw new AssertionFailure( "Unknown fetch mode" );
			}
		}
	}

	public static FetchStyle annotationFetchModeToFetchStyle(org.hibernate.annotations.FetchMode annotationFetchMode) {
		switch ( annotationFetchMode ) {
			case JOIN: {
				return FetchStyle.JOIN;
			}
			case SELECT: {
				return FetchStyle.SELECT;
			}
			case SUBSELECT: {
				return FetchStyle.SUBSELECT;
			}
			default: {
				throw new AssertionFailure( "Unknown fetch mode" );
			}
		}
	}

	public static Set<CascadeStyle> cascadeTypeToCascadeStyleSet(
			Set<CascadeType> cascadeTypes,
			Set<org.hibernate.annotations.CascadeType> hibernateCascadeTypes,
			EntityBindingContext context) {
		Set<CascadeStyle> cascadeStyleSet = new HashSet<CascadeStyle>();
		if ( CollectionHelper.isEmpty( cascadeTypes )
				&& CollectionHelper.isEmpty( hibernateCascadeTypes ) ) {
			String cascades = context.getMappingDefaults().getCascadeStyle();
			for ( String cascade : StringHelper.split( ",", cascades ) ) {
				cascadeStyleSet.add( CascadeStyles.getCascadeStyle( cascade ) );
			}
		}
		else {
			for ( CascadeType cascadeType : cascadeTypes ) {
				cascadeStyleSet.add( cascadeTypeToCascadeStyle( cascadeType ) );
			}
			for ( org.hibernate.annotations.CascadeType cascadeType : hibernateCascadeTypes ) {
				cascadeStyleSet.add( cascadeTypeToCascadeStyle( cascadeType ) );
			}
		}
		return cascadeStyleSet;
	}
}


