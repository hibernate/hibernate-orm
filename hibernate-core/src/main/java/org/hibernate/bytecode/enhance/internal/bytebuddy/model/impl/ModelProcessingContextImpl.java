/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.hibernate.bytecode.enhance.internal.bytebuddy.ClassFileLocatorImpl;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelProcessingContext;

import jakarta.persistence.AttributeConverter;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;

/**
 * @author Steve Ebersole
 */
public class ModelProcessingContextImpl implements ModelProcessingContext {
	private final ClassDetailsRegistryImpl classDetailsRegistry;
	private final ClassFileLocatorImpl classFileLocator;
	private final TypePool typePool;

	public ModelProcessingContextImpl(
			ClassFileLocatorImpl classFileLocator,
			TypePool typePool) {
		this.classFileLocator = classFileLocator;
		this.typePool = typePool;
		final ClassDetailsBuilderImpl classDetailsBuilder = new ClassDetailsBuilderImpl( this );
		this.classDetailsRegistry = new ClassDetailsRegistryImpl( classDetailsBuilder, this );

		primeClassDetails( String.class );
		primeClassDetails( Boolean.class );
		primeClassDetails( Enum.class );
		primeClassDetails( Byte.class );
		primeClassDetails( Short.class );
		primeClassDetails( Integer.class );
		primeClassDetails( Long.class );
		primeClassDetails( Double.class );
		primeClassDetails( Float.class );
		primeClassDetails( BigInteger.class );
		primeClassDetails( BigDecimal.class );
		primeClassDetails( Blob.class );
		primeClassDetails( Clob.class );
		primeClassDetails( NClob.class );
		primeClassDetails( Instant.class );
		primeClassDetails( LocalDate.class );
		primeClassDetails( LocalTime.class );
		primeClassDetails( LocalDateTime.class );
		primeClassDetails( OffsetTime.class );
		primeClassDetails( OffsetDateTime.class );
		primeClassDetails( ZonedDateTime.class );
		primeClassDetails( java.util.Date.class );
		primeClassDetails( java.sql.Date.class );
		primeClassDetails( java.sql.Time.class );
		primeClassDetails( java.sql.Timestamp.class );
		primeClassDetails( URL.class );
		primeClassDetails( Collection.class );
		primeClassDetails( Set.class );
		primeClassDetails( List.class );
		primeClassDetails( Map.class );
		primeClassDetails( Comparator.class );
		primeClassDetails( Comparable.class );
		primeClassDetails( SortedSet.class );
		primeClassDetails( SortedMap.class );

		primeClassDetails( AttributeConverter.class );
	}

	private void primeClassDetails(Class<?> javaType) {
		classDetailsRegistry.resolveClassDetails( javaType.getName() );
	}

	@Override
	public ClassDetailsRegistryImpl getClassDetailsRegistry() {
		return classDetailsRegistry;
	}

	@Override
	public TypePool getTypePool() {
		return typePool;
	}

	@Override
	public ClassFileLocator getClassFileLocator() {
		return classFileLocator;
	}
}
