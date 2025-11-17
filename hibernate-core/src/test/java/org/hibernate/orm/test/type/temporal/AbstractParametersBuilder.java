/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.temporal;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Steve Ebersole
 */
public class AbstractParametersBuilder<V,D extends Data<V>,B extends AbstractParametersBuilder<V,D,B>> {
	protected final Dialect dialect;

	private final List<Parameter<V,D>> result = new ArrayList<>();

	private final List<Class<? extends AbstractRemappingH2Dialect>> remappingDialectClasses = new ArrayList<>();

	private ZoneId forcedJdbcTimeZone = null;

	protected AbstractParametersBuilder(Dialect dialect) {
		this.dialect = dialect;
		// Always test without remapping
		remappingDialectClasses.add( null );
	}

	public B skippedForDialects(List<Class<?>> dialectsToSkip, Consumer<B> ifNotSkipped) {
		boolean skip = false;
		for ( Class<?> dialectClass : dialectsToSkip ) {
			if ( dialectClass.isInstance( dialect ) ) {
				skip = true;
				break;
			}
		}
		if ( !skip ) {
			ifNotSkipped.accept( thisAsB() );
		}
		return thisAsB();
	}

	public B skippedForDialects(Predicate<Dialect> skipPredicate, Consumer<B> ifNotSkipped) {
		if ( !skipPredicate.test( dialect ) ) {
			ifNotSkipped.accept( thisAsB() );
		}
		return thisAsB();
	}

	public B withForcedJdbcTimezone(String zoneIdString, Consumer<B> contributor) {
		this.forcedJdbcTimeZone = ZoneId.of( zoneIdString );
		try {
			contributor.accept( thisAsB() );
		}
		finally {
			this.forcedJdbcTimeZone = null;
		}
		return thisAsB();
	}

	@SafeVarargs
	public final B alsoTestRemappingsWithH2(Class<? extends AbstractRemappingH2Dialect>... dialectClasses) {
		if ( dialect instanceof H2Dialect && !( (H2Dialect) dialect ).hasOddDstBehavior() ) {
			// Only test remappings with H2
			Collections.addAll( remappingDialectClasses, dialectClasses );
		}
		return thisAsB();
	}

	protected final boolean isNanosecondPrecisionSupported() {
		// This used to return true for H2Dialect, but as of 1.4.197 h2 does not use ns precision by default anymore.
		// Bringing back ns precision would require timestamp(9) in the dialect class.
		return false;
	}

	protected final B add(ZoneId defaultJvmTimeZone, D testData) {
		for ( Class<? extends AbstractRemappingH2Dialect> remappingDialectClass : remappingDialectClasses ) {
			addParam( defaultJvmTimeZone, forcedJdbcTimeZone, remappingDialectClass, testData );
		}

		if ( forcedJdbcTimeZone == null ) {
			for ( ZoneId hibernateJdbcTimeZone : getHibernateJdbcTimeZonesToTest() ) {
				addParam( defaultJvmTimeZone, hibernateJdbcTimeZone, null, testData );
			}
		}
		return thisAsB();
	}

	private void addParam(
			ZoneId defaultJvmTimeZone,
			ZoneId forcedJdbcTimeZone,
			Class<? extends AbstractRemappingH2Dialect> remappingDialectClass,
			D testData) {
		result.add( new Parameter<>(
				new Environment( defaultJvmTimeZone, forcedJdbcTimeZone, remappingDialectClass ),
				testData
		) );

	}

	protected Iterable<? extends ZoneId> getHibernateJdbcTimeZonesToTest() {
		return Arrays.asList( Timezones.ZONE_GMT, Timezones.ZONE_OSLO );
	}

	private B thisAsB() {
		//noinspection unchecked
		return (B) this;
	}

	public List<Parameter<V,D>> build() {
		return result;
	}
}
