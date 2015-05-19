/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleWithSubselectEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyWithExtraEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOuterJoinEnum;
import org.hibernate.boot.model.source.spi.FetchCharacteristicsPluralAttribute;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;

/**
 * @author Steve Ebersole
 */
public class FetchCharacteristicsPluralAttributeImpl implements FetchCharacteristicsPluralAttribute {
	private final FetchTiming fetchTiming;
	private final FetchStyle fetchStyle;

	private final Integer batchSize;
	private boolean extraLazy;

	public FetchCharacteristicsPluralAttributeImpl(
			FetchTiming fetchTiming,
			FetchStyle fetchStyle,
			Integer batchSize,
			boolean extraLazy) {
		this.fetchTiming = fetchTiming;
		this.fetchStyle = fetchStyle;
		this.batchSize = batchSize;
		this.extraLazy = extraLazy;
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
	public Integer getBatchSize() {
		return batchSize;
	}

	@Override
	public boolean isExtraLazy() {
		return getFetchTiming() == FetchTiming.DELAYED && extraLazy;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Builder

	public static class Builder {
		private FetchTiming fetchTiming;
		private FetchStyle fetchStyle;
		private Integer batchSize;
		private boolean extraLazy;

		public Builder(MappingDefaults mappingDefaults) {
			setFetchStyle( FetchStyle.SELECT );
			if ( mappingDefaults.areCollectionsImplicitlyLazy() ) {
				setFetchTiming( FetchTiming.DELAYED );
			}
			else {
				setFetchTiming( FetchTiming.IMMEDIATE );
			}
		}

		public Builder setFetchTiming(FetchTiming fetchTiming) {
			this.fetchTiming = fetchTiming;
			return this;
		}

		public Builder setFetchStyle(FetchStyle fetchStyle) {
			this.fetchStyle = fetchStyle;
			return this;
		}

		public Builder setBatchSize(Integer batchSize) {
			this.batchSize = batchSize;
			return this;
		}

		public void setExtraLazy(boolean extraLazy) {
			this.extraLazy = extraLazy;
		}

		public FetchCharacteristicsPluralAttributeImpl createPluralAttributeFetchCharacteristics() {
			return new FetchCharacteristicsPluralAttributeImpl( fetchTiming, fetchStyle, batchSize, extraLazy );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Static builder methods

	public static FetchCharacteristicsPluralAttributeImpl interpret(
			MappingDefaults mappingDefaults,
			JaxbHbmFetchStyleWithSubselectEnum fetch,
			JaxbHbmOuterJoinEnum outerJoin,
			JaxbHbmLazyWithExtraEnum lazy,
			int batchSize) {
		Builder builder = new Builder( mappingDefaults );

		// #initOuterJoinFetchSetting
		if ( fetch == null ) {
			//noinspection StatementWithEmptyBody
			if ( outerJoin == null ) {
				// use the defaults set above
			}
			else {
				// use old (HB 2.1) defaults if outer-join is specified
				if ( outerJoin == JaxbHbmOuterJoinEnum.TRUE ) {
					builder.setFetchStyle( FetchStyle.JOIN );
				}
			}
		}
		else {
			if ( fetch == JaxbHbmFetchStyleWithSubselectEnum.SUBSELECT ) {
				builder.setFetchStyle( FetchStyle.SUBSELECT );
			}
			else if ( fetch == JaxbHbmFetchStyleWithSubselectEnum.JOIN ) {
				builder.setFetchStyle( FetchStyle.JOIN );
			}
		}


		if ( lazy != null ) {
			if ( lazy ==JaxbHbmLazyWithExtraEnum.TRUE ) {
				builder.setFetchTiming( FetchTiming.DELAYED );
			}
			else if ( lazy == JaxbHbmLazyWithExtraEnum.FALSE ) {
				builder.setFetchTiming( FetchTiming.IMMEDIATE );
			}
			else if ( lazy == JaxbHbmLazyWithExtraEnum.EXTRA ) {
				builder.setFetchTiming( FetchTiming.DELAYED );
				builder.setExtraLazy( true );
			}
		}

		builder.setBatchSize( batchSize );
		if ( batchSize > 0 ) {
			if ( builder.fetchStyle == FetchStyle.JOIN || builder.fetchStyle == FetchStyle.SELECT ) {
				builder.setFetchStyle( FetchStyle.BATCH );
			}
		}

		return builder.createPluralAttributeFetchCharacteristics();
	}
}
