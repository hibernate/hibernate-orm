/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ModelsContext;

/**
 * Base support for {@linkplain DialectOverrider} annotations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractOverrider<O extends Annotation> implements DialectOverrider<O> {
	private Class<? extends Dialect> dialect;
	private DialectOverride.Version before;
	private DialectOverride.Version sameOrAfter;

	public AbstractOverrider() {
	}

	public AbstractOverrider(
			Map<String, Object> attributeValues,
			AnnotationDescriptor<?> descriptor,
			ModelsContext modelContext) {
		dialect( (Class<? extends Dialect>) attributeValues.get( "dialect" ) );
		before( (DialectOverride.Version) attributeValues.get( "before" ) );
		sameOrAfter( (DialectOverride.Version) attributeValues.get( "sameOrAfter" ) );
	}

	@Override
	public Class<? extends Dialect> dialect() {
		return dialect;
	}

	public void dialect(Class<? extends Dialect> dialect) {
		this.dialect = dialect;
	}

	@Override
	public DialectOverride.Version before() {
		return before;
	}

	public void before(DialectOverride.Version before) {
		this.before = before;
	}

	@Override
	public DialectOverride.Version sameOrAfter() {
		return sameOrAfter;
	}

	public void sameOrAfter(DialectOverride.Version sameOrAfter) {
		this.sameOrAfter = sameOrAfter;
	}

	@Override
	public boolean matches(Dialect dialectToMatch) {
		if ( !dialect().isAssignableFrom( dialectToMatch.getClass() ) ) {
			return false;
		}

		final DatabaseVersion versionToMatch = dialectToMatch.getVersion();
		return versionToMatch.isBefore( before().major(), before().minor() )
				&& versionToMatch.isSameOrAfter( sameOrAfter().major(), sameOrAfter().minor() );
	}
}
