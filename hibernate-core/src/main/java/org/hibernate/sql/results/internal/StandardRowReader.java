/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingResolution;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class StandardRowReader<T> implements RowReader<T> {
	private final DomainResultAssembler<?>[] resultAssemblers;
	private final Initializer<InitializerData>[] resultInitializers;
	private final InitializerData[] resultInitializersData;
	private final Initializer<InitializerData>[] initializers;
	private final InitializerData[] initializersData;
	private final Initializer<InitializerData>[] sortedForResolveInstance;
	private final InitializerData[] sortedForResolveInstanceData;
	private final boolean hasCollectionInitializers;
	private final @Nullable RowTransformer<T> rowTransformer;
	private final Class<T> domainResultJavaType;

	private final ComponentType componentType;
	private final Class<?> resultElementClass;

	public StandardRowReader(
			JdbcValuesMappingResolution jdbcValuesMappingResolution,
			RowTransformer<T> rowTransformer,
			Class<T> domainResultJavaType) {
		this(
				jdbcValuesMappingResolution.getDomainResultAssemblers(),
				jdbcValuesMappingResolution.getResultInitializers(),
				jdbcValuesMappingResolution.getInitializers(),
				jdbcValuesMappingResolution.getSortedForResolveInstance(),
				jdbcValuesMappingResolution.hasCollectionInitializers(),
				rowTransformer,
				domainResultJavaType
		);
	}

	public StandardRowReader(
			DomainResultAssembler<?>[] resultAssemblers,
			Initializer<?>[] resultInitializers,
			Initializer<?>[] initializers,
			Initializer<?>[] sortedForResolveInitializers,
			boolean hasCollectionInitializers,
			RowTransformer<T> rowTransformer,
			Class<T> domainResultJavaType) {
		this.resultAssemblers = resultAssemblers;
		this.resultInitializers = (Initializer<InitializerData>[]) resultInitializers;
		this.resultInitializersData = new InitializerData[resultInitializers.length];
		this.initializers = (Initializer<InitializerData>[]) initializers;
		this.initializersData = new InitializerData[initializers.length];
		this.sortedForResolveInstance = (Initializer<InitializerData>[]) sortedForResolveInitializers;
		this.sortedForResolveInstanceData = new InitializerData[sortedForResolveInstance.length];
		this.hasCollectionInitializers = hasCollectionInitializers;
		this.rowTransformer = rowTransformer == RowTransformerArrayImpl.instance() && resultAssemblers.length != 1
				|| rowTransformer == RowTransformerStandardImpl.instance()
				|| rowTransformer == RowTransformerSingularReturnImpl.instance() && resultAssemblers.length == 1
				? null
				: rowTransformer;
		this.domainResultJavaType = domainResultJavaType;
		if ( domainResultJavaType == null
				|| domainResultJavaType == Object[].class
				|| domainResultJavaType == Object.class
				|| !domainResultJavaType.isArray()
				|| resultAssemblers.length == 1
				&& domainResultJavaType == resultAssemblers[0].getAssembledJavaType().getJavaTypeClass() ) {
			this.resultElementClass = Object.class;
			this.componentType = ComponentType.OBJECT;
		}
		else {
			this.resultElementClass = domainResultJavaType.getComponentType();
			this.componentType = ComponentType.determineComponentType( domainResultJavaType );
		}
	}

	@Override
	public Class<T> getDomainResultResultJavaType() {
		return domainResultJavaType;
	}

	@Override
	public List<@Nullable JavaType<?>> getResultJavaTypes() {
		List<JavaType<?>> javaTypes = new ArrayList<>( resultAssemblers.length );
		for ( DomainResultAssembler resultAssembler : resultAssemblers ) {
			javaTypes.add( resultAssembler.getAssembledJavaType() );
		}
		return javaTypes;
	}

	@Override
	public int getInitializerCount() {
		return initializers.length;
	}

	@Override
	public @Nullable EntityKey resolveSingleResultEntityKey(RowProcessingState rowProcessingState) {
		final EntityInitializer<?> entityInitializer = resultInitializers.length == 0
				? null
				: resultInitializers[0].asEntityInitializer();
		if ( entityInitializer == null ) {
			return null;
		}
		final EntityKey entityKey = entityInitializer.resolveEntityKeyOnly( rowProcessingState );
		finishUpRow();
		return entityKey;
	}

	@Override
	public boolean hasCollectionInitializers() {
		return hasCollectionInitializers;
	}

	@Override
	@AllowReflection
	public T readRow(RowProcessingState rowProcessingState) {
		coordinateInitializers( rowProcessingState );

		final T result;
		if ( componentType != ComponentType.OBJECT ) {
			result = readPrimitiveRow( rowProcessingState );
		}
		else {
			if ( resultAssemblers.length == 1 && rowTransformer == null ) {
				//noinspection unchecked
				result = (T) resultAssemblers[0].assemble( rowProcessingState );
			}
			else {
				final Object[] resultRow = (Object[]) Array.newInstance( resultElementClass, resultAssemblers.length );
				for ( int i = 0; i < resultAssemblers.length; i++ ) {
					resultRow[i] = resultAssemblers[i].assemble( rowProcessingState );
				}
				//noinspection unchecked
				result = rowTransformer == null
						? (T) resultRow
						: rowTransformer.transformRow( resultRow );
			}
		}

		finishUpRow();
		return result;
	}

	private T readPrimitiveRow(RowProcessingState rowProcessingState) {
		// The following is ugly, but unfortunately necessary to not hurt performance.
		// This implementation was micro-benchmarked and discussed with Francesco Nigro,
		// who hinted that using this style instead of the reflective Array.getLength(), Array.set()
		// is easier for the JVM to optimize
		switch ( componentType ) {
			case BOOLEAN:
				final boolean[] resultBooleanRow = new boolean[resultAssemblers.length];
				for ( int i = 0; i < resultAssemblers.length; i++ ) {
					resultBooleanRow[i] = (boolean) resultAssemblers[i].assemble( rowProcessingState );
				}
				return (T) resultBooleanRow;
			case BYTE:
				final byte[] resultByteRow = new byte[resultAssemblers.length];
				for ( int i = 0; i < resultAssemblers.length; i++ ) {
					resultByteRow[i] = (byte) resultAssemblers[i].assemble( rowProcessingState );
				}
				return (T) resultByteRow;
			case CHAR:
				final char[] resultCharRow = new char[resultAssemblers.length];
				for ( int i = 0; i < resultAssemblers.length; i++ ) {
					resultCharRow[i] = (char) resultAssemblers[i].assemble( rowProcessingState );
				}
				return (T) resultCharRow;
			case SHORT:
				final short[] resultShortRow = new short[resultAssemblers.length];
				for ( int i = 0; i < resultAssemblers.length; i++ ) {
					resultShortRow[i] = (short) resultAssemblers[i].assemble( rowProcessingState );
				}
				return (T) resultShortRow;
			case INT:
				final int[] resultIntRow = new int[resultAssemblers.length];
				for ( int i = 0; i < resultAssemblers.length; i++ ) {
					resultIntRow[i] = (int) resultAssemblers[i].assemble( rowProcessingState );
				}
				return (T) resultIntRow;
			case LONG:
				final long[] resultLongRow = new long[resultAssemblers.length];
				for ( int i = 0; i < resultAssemblers.length; i++ ) {
					resultLongRow[i] = (long) resultAssemblers[i].assemble( rowProcessingState );
				}
				return (T) resultLongRow;
			case FLOAT:
				final float[] resultFloatRow = new float[resultAssemblers.length];
				for ( int i = 0; i < resultAssemblers.length; i++ ) {
					resultFloatRow[i] = (float) resultAssemblers[i].assemble( rowProcessingState );
				}
				return (T) resultFloatRow;
			case DOUBLE:
				final double[] resultDoubleRow = new double[resultAssemblers.length];
				for ( int i = 0; i < resultAssemblers.length; i++ ) {
					resultDoubleRow[i] = (double) resultAssemblers[i].assemble( rowProcessingState );
				}
				return (T) resultDoubleRow;
			default:
				throw new AssertionError( "Object should be handled specially" );
		}
	}

	private void finishUpRow() {
		for ( InitializerData data : initializersData ) {
			data.setState( Initializer.State.UNINITIALIZED );
		}
	}

	private void coordinateInitializers(RowProcessingState rowProcessingState) {
		for ( int i = 0; i < resultInitializers.length; i++ ) {
			resultInitializers[i].resolveKey( resultInitializersData[i] );
		}
		for ( int i = 0; i < sortedForResolveInstance.length; i++ ) {
			if ( sortedForResolveInstanceData[i].getState() == Initializer.State.KEY_RESOLVED ) {
				sortedForResolveInstance[i].resolveInstance( sortedForResolveInstanceData[i] );
			}
		}
		for ( int i = 0; i < initializers.length; i++ ) {
			if ( initializersData[i].getState() == Initializer.State.RESOLVED ) {
				initializers[i].initializeInstance( initializersData[i] );
			}
		}
	}

	@Override
	public void startLoading(RowProcessingState processingState) {
		for ( int i = 0; i < resultInitializers.length; i++ ) {
			final Initializer<?> initializer = resultInitializers[i];
			initializer.startLoading( processingState );
			resultInitializersData[i] = initializer.getData( processingState );
		}
		for ( int i = 0; i < sortedForResolveInstance.length; i++ ) {
			sortedForResolveInstanceData[i] = sortedForResolveInstance[i].getData( processingState );
		}
		for ( int i = 0; i < initializers.length; i++ ) {
			initializersData[i] = initializers[i].getData( processingState );
		}
	}

	@Override
	public void finishUp(RowProcessingState rowProcessingState) {
		for ( int i = 0; i < initializers.length; i++ ) {
			initializers[i].endLoading( initializersData[i] );
		}
	}

	enum ComponentType {
		BOOLEAN(boolean.class),
		BYTE(byte.class),
		SHORT(short.class),
		CHAR(char.class),
		INT(int.class),
		LONG(long.class),
		FLOAT(float.class),
		DOUBLE(double.class),
		OBJECT(Object.class);

		private final Class<?> componentType;

		ComponentType(Class<?> componentType) {
			this.componentType = componentType;
		}

		public static ComponentType determineComponentType(Class<?> resultType) {
			if ( resultType == boolean[].class) {
				return BOOLEAN;
			}
			else if ( resultType == byte[].class) {
				return BYTE;
			}
			else if ( resultType == short[].class) {
				return SHORT;
			}
			else if ( resultType == char[].class) {
				return CHAR;
			}
			else if ( resultType == int[].class) {
				return INT;
			}
			else if ( resultType == long[].class) {
				return LONG;
			}
			else if ( resultType == float[].class) {
				return FLOAT;
			}
			else if ( resultType == double[].class) {
				return DOUBLE;
			}
			return OBJECT;
		}

		public Class<?> getComponentType() {
			return componentType;
		}
	}

}
