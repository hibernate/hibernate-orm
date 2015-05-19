/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyWithNoProxyEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOuterJoinEnum;
import org.hibernate.boot.model.source.spi.FetchCharacteristicsSingularAssociation;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.log.DeprecationLogger;

/**
 * {@code hbm.xml} specific handling for FetchCharacteristicsSingularAssociation
 *
 * @author Steve Ebersole
 */
public class FetchCharacteristicsSingularAssociationImpl implements FetchCharacteristicsSingularAssociation {
	private final FetchTiming fetchTiming;
	private final FetchStyle fetchStyle;

	private final boolean unwrapProxies;

	private FetchCharacteristicsSingularAssociationImpl(
			FetchTiming fetchTiming,
			FetchStyle fetchStyle,
			boolean unwrapProxies) {
		this.fetchTiming = fetchTiming;
		this.fetchStyle = fetchStyle;
		this.unwrapProxies = unwrapProxies;
	}

	@Override
	public FetchTiming getFetchTiming() {
		return fetchTiming;
	}

	@Override
	public FetchStyle getFetchStyle() {
		return fetchStyle;
	}

	@Override
	public boolean isUnwrapProxies() {
		return unwrapProxies;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Builder

	public static class Builder {
		private FetchTiming fetchTiming;
		private FetchStyle fetchStyle;
		private boolean unwrapProxies;

		@SuppressWarnings("UnusedParameters")
		public Builder(MappingDefaults mappingDefaults) {
			//
			// todo : may need to add back a concept of DEFAULT fetch style / timing.
			// 		one option I like is adding a fetchTiming / fetchStyle and
			//		effectiveFetchTiming / effectiveFetchStyle.  The reasoning here
			//		is for Loaders and LoadPLan building

			fetchTiming = FetchTiming.DELAYED;
			fetchStyle = FetchStyle.SELECT;
		}

		public Builder setFetchTiming(FetchTiming fetchTiming) {
			this.fetchTiming = fetchTiming;
			return this;
		}

		public Builder setFetchStyle(FetchStyle fetchStyle) {
			this.fetchStyle = fetchStyle;
			return this;
		}

		public Builder setUnwrapProxies(boolean unwrapProxies) {
			this.unwrapProxies = unwrapProxies;
			return this;
		}

		public FetchCharacteristicsSingularAssociationImpl createFetchCharacteristics() {
			return new FetchCharacteristicsSingularAssociationImpl( fetchTiming, fetchStyle, unwrapProxies );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Static builder methods

	public static FetchCharacteristicsSingularAssociationImpl interpretManyToOne(
			MappingDefaults mappingDefaults,
			JaxbHbmFetchStyleEnum fetchMapping,
			JaxbHbmOuterJoinEnum outerJoinMapping,
			JaxbHbmLazyWithNoProxyEnum lazyMapping) {
		final Builder builder = new Builder( mappingDefaults );

		// this is taken verbatim from the old HbmBinder (except that
		// defaults are handled in the Builder ctor)

		// #initOuterJoinFetchSetting
		if ( fetchMapping == null ) {
			if ( outerJoinMapping == null ) {
				builder.setFetchStyle( FetchStyle.SELECT );
			}
			else {
				switch ( outerJoinMapping ) {
					case TRUE: {
						builder.setFetchStyle( FetchStyle.JOIN );
						break;
					}
					default: {
						builder.setFetchStyle( FetchStyle.SELECT );
						break;
					}
				}
			}
		}
		else {
			// only defined SELECT and JOIN
			if ( fetchMapping == JaxbHbmFetchStyleEnum.JOIN ) {
				builder.setFetchStyle( FetchStyle.JOIN );
				// as much as I think this makes sense, its not what
				// Hibernate did historically :(
				//builder.setFetchTiming( FetchTiming.IMMEDIATE );
			}
			else {
				builder.setFetchStyle( FetchStyle.SELECT );
			}
		}

		// #initLaziness
		if ( lazyMapping != null ) {
			if ( lazyMapping == JaxbHbmLazyWithNoProxyEnum.NO_PROXY ) {
				builder.setFetchTiming( FetchTiming.DELAYED );
				builder.setUnwrapProxies( true );
			}
			else if ( lazyMapping == JaxbHbmLazyWithNoProxyEnum.PROXY ) {
				builder.setFetchTiming( FetchTiming.DELAYED );
			}
			else if ( lazyMapping == JaxbHbmLazyWithNoProxyEnum.FALSE ) {
				builder.setFetchTiming( FetchTiming.IMMEDIATE );
			}
		}

		return builder.createFetchCharacteristics();
	}


	public static FetchCharacteristicsSingularAssociationImpl interpretManyToManyElement(
			MappingDefaults mappingDefaults,
			JaxbHbmFetchStyleEnum fetchMapping,
			JaxbHbmOuterJoinEnum outerJoinMapping,
			JaxbHbmLazyEnum lazyMapping) {
		final Builder builder = new Builder( mappingDefaults );

		// #initOuterJoinFetchSetting
		if ( fetchMapping == null ) {
			if ( outerJoinMapping == null ) {
				builder.setFetchTiming( FetchTiming.IMMEDIATE );
				builder.setFetchStyle( FetchStyle.JOIN );
			}
			else {
				//NOTE <many-to-many outer-join="..." is deprecated.:
				// Default to join and non-lazy for the "second join"
				// of the many-to-many
				DeprecationLogger.DEPRECATION_LOGGER.deprecatedManyToManyOuterJoin();
				builder.setFetchTiming( FetchTiming.IMMEDIATE );
				builder.setFetchStyle( FetchStyle.JOIN );
			}
		}
		else {
			//NOTE <many-to-many fetch="..." is deprecated.:
			// Default to join and non-lazy for the "second join"
			// of the many-to-many
			DeprecationLogger.DEPRECATION_LOGGER.deprecatedManyToManyFetch();
			builder.setFetchTiming( FetchTiming.IMMEDIATE );
			builder.setFetchStyle( FetchStyle.JOIN );
		}

		// #initLaziness
		if ( lazyMapping != null ) {
			if ( lazyMapping == JaxbHbmLazyEnum.FALSE ) {
				builder.setFetchTiming( FetchTiming.IMMEDIATE );
			}
		}

		return builder.createFetchCharacteristics();
	}

	public static FetchCharacteristicsSingularAssociationImpl interpretOneToOne(
			MappingDefaults mappingDefaults,
			JaxbHbmFetchStyleEnum fetchMapping,
			JaxbHbmOuterJoinEnum outerJoinMapping,
			JaxbHbmLazyWithNoProxyEnum lazyMapping,
			boolean constrained) {
		final Builder builder = new Builder( mappingDefaults );

		// #initOuterJoinFetchSetting
		if ( fetchMapping == null ) {
			if ( outerJoinMapping == null ) {
				if ( !constrained ) {
					// one-to-one constrained=false cannot be proxied,
					// so default to join and non-lazy
					builder.setFetchTiming( FetchTiming.IMMEDIATE );
					builder.setFetchStyle( FetchStyle.JOIN );
				}
				else {
					builder.setFetchStyle( FetchStyle.SELECT );
				}
			}
			else {
				switch ( outerJoinMapping ) {
					case TRUE: {
						builder.setFetchStyle( FetchStyle.JOIN );
						break;
					}
					default: {
						builder.setFetchStyle( FetchStyle.SELECT );
						break;
					}
				}
			}
		}
		else {
			// only defined SELECT and JOIN
			if ( fetchMapping == JaxbHbmFetchStyleEnum.JOIN ) {
				builder.setFetchStyle( FetchStyle.JOIN );
				// as much as I think this makes sense, its not what
				// Hibernate did historically :(
				//builder.setFetchTiming( FetchTiming.IMMEDIATE );
			}
			else {
				builder.setFetchStyle( FetchStyle.SELECT );
			}
		}

		// #initLaziness
		if ( lazyMapping != null ) {
			if ( lazyMapping == JaxbHbmLazyWithNoProxyEnum.NO_PROXY ) {
				builder.setFetchTiming( FetchTiming.DELAYED );
				builder.setUnwrapProxies( true );
			}
			else if ( lazyMapping == JaxbHbmLazyWithNoProxyEnum.PROXY ) {
				builder.setFetchTiming( FetchTiming.DELAYED );
			}
			else if ( lazyMapping == JaxbHbmLazyWithNoProxyEnum.FALSE ) {
				builder.setFetchTiming( FetchTiming.IMMEDIATE );
			}
		}

		return builder.createFetchCharacteristics();
	}

}
