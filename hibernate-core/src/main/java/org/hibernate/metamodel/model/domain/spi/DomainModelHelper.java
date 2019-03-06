/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.StringTokenizer;

import org.hibernate.FetchMode;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.spi.MetamodelImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class DomainModelHelper {
	private static final Logger log = Logger.getLogger( DomainModelHelper.class );

	@SuppressWarnings("unchecked")
	public static <T, S extends T> ManagedTypeDescriptor<S> resolveSubType(
			ManagedTypeDescriptor<T> baseType,
			String subTypeName,
			SessionFactoryImplementor sessionFactory) {
		final MetamodelImplementor metamodel = sessionFactory.getMetamodel();

		if ( baseType instanceof EmbeddedTypeDescriptor<?> ) {
			// todo : at least validate the string is a valid sub-type of the embeddable class?
			return (ManagedTypeDescriptor) baseType;
		}

		final String importedClassName = metamodel.getImportedName( subTypeName );
		if ( importedClassName != null ) {
			// first, try to find it by name directly..
			ManagedTypeDescriptor<S> subManagedType = metamodel.entity( importedClassName );
			if ( subManagedType != null ) {
				return subManagedType;
			}

			// it could still be a mapped-superclass
			try {
				final Class<S> subTypeClass = sessionFactory.getServiceRegistry()
						.getService( ClassLoaderService.class )
						.classForName( importedClassName );

				return metamodel.managedType( subTypeClass );
			}
			catch (Exception ignore) {
			}
		}

		throw new IllegalArgumentException( "Unknown sub-type name (" + baseType.getDomainTypeName() + ") : " + subTypeName );
	}

	public static <S> ManagedTypeDescriptor<S> resolveSubType(
			ManagedTypeDescriptor<? super S> baseType,
			Class<S> subTypeClass,
			SessionFactoryImplementor sessionFactory) {
		// todo : validate the hierarchy-ness...
		final MetamodelImplementor metamodel = sessionFactory.getMetamodel();
		return metamodel.managedType( subTypeClass );
	}


	public static FetchStrategy determineFetchStrategy(
			PersistentAttributeMapping bootModelAttribute,
			ManagedTypeDescriptor runtimeModelContainer,
			EntityTypeDescriptor entityDescriptor) {

		FetchStyle style = determineStyle(
				bootModelAttribute,
				entityDescriptor
		);

		return new FetchStrategy(
				determineTiming(
						style,
						runtimeModelContainer
				),
				style
		);
	}

	private static FetchTiming determineTiming(
			FetchStyle style,
			ManagedTypeDescriptor runtimeModelContainer) {
		switch ( style ) {
			case JOIN: {
				return FetchTiming.IMMEDIATE;
			}
			case BATCH:
			case SUBSELECT: {
				return FetchTiming.DELAYED;
			}
			default: {
				// SELECT case, can be either
				if ( runtimeModelContainer instanceof EntityTypeDescriptor ) {
					final EntityTypeDescriptor container = (EntityTypeDescriptor) runtimeModelContainer;
					if ( !container.hasProxy() && !container.getBytecodeEnhancementMetadata()
							.isEnhancedForLazyLoading() ) {
						return FetchTiming.IMMEDIATE;
					}
					else {
						return FetchTiming.DELAYED;
					}
				}
				else {
					return FetchTiming.DELAYED;
				}
			}
		}
	}

	private static FetchStyle determineStyle(
			PersistentAttributeMapping bootModelAttribute,
			EntityTypeDescriptor entityDescriptor) {
		// todo (6.0) : allow subselect fetching for entity refs

		final FetchMode fetchMode = bootModelAttribute.getValueMapping().getFetchMode();

		switch ( fetchMode ) {
			case JOIN: {
				if ( bootModelAttribute.isLazy() ) {
					log.debugf(
							"%s.%s defined join fetch and lazy",
							bootModelAttribute.getEntity().getEntityName(),
							bootModelAttribute.getName()
					);
				}
				return FetchStyle.JOIN;
			}
			default: {
				return FetchStyle.SELECT;
			}
		}
	}

	public static FetchStrategy determineFetchStrategy(Collection bootCollectionDescriptor) {
		FetchStyle style = determineStyle( bootCollectionDescriptor );
		return new FetchStrategy(
				determineTiming( bootCollectionDescriptor, style ),
				style
		);
	}

	private static FetchTiming determineTiming(Collection bootCollectionDescriptor, FetchStyle style) {
		switch ( style ) {
			case JOIN: {
				return FetchTiming.IMMEDIATE;
			}
			case BATCH:
			case SUBSELECT: {
				return FetchTiming.DELAYED;
			}
			default: {
				// SELECT case, can be either
				return bootCollectionDescriptor.isLazy() || bootCollectionDescriptor.isExtraLazy()
						? FetchTiming.DELAYED
						: FetchTiming.IMMEDIATE;
			}
		}
	}

	private static FetchStyle determineStyle(Collection bootCollectionDescriptor) {
		if ( bootCollectionDescriptor.getBatchSize() > 1 ) {
			return FetchStyle.BATCH;
		}

		if ( bootCollectionDescriptor.isSubselectLoadable() ) {
			return FetchStyle.SUBSELECT;
		}

		final FetchMode fetchMode = bootCollectionDescriptor.getFetchMode();

		if ( fetchMode == FetchMode.JOIN ) {
			return FetchStyle.JOIN;
		}

		return FetchStyle.SELECT;
	}

	public static CascadeStyle determineCascadeStyle(String cascade) {
		if ( cascade == null || cascade.equals( "none" ) ) {
			return CascadeStyles.NONE;
		}
		else {
			StringTokenizer tokens = new StringTokenizer( cascade, ", " );
			CascadeStyle[] styles = new CascadeStyle[ tokens.countTokens() ];
			int i = 0;
			while ( tokens.hasMoreTokens() ) {
				styles[ i++ ] = CascadeStyles.getCascadeStyle( tokens.nextToken() );
			}
			return new CascadeStyles.MultipleCascadeStyle( styles );
		}
	}
}
