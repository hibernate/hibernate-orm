/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.util.concurrent.TimeUnit;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
public class PackageInfoCacheBenchmark {
	private static final Class<?>[] ENTITY_CLASSES = {
			Entity01.class, Entity02.class, Entity03.class, Entity04.class,
			Entity05.class, Entity06.class, Entity07.class, Entity08.class,
			Entity09.class, Entity10.class, Entity11.class, Entity12.class,
			Entity13.class, Entity14.class, Entity15.class, Entity16.class,
			Entity17.class, Entity18.class, Entity19.class, Entity20.class,
			Entity21.class, Entity22.class, Entity23.class, Entity24.class,
			Entity25.class, Entity26.class, Entity27.class, Entity28.class,
			Entity29.class, Entity30.class, Entity31.class, Entity32.class,
			Entity33.class, Entity34.class, Entity35.class, Entity36.class,
			Entity37.class, Entity38.class, Entity39.class, Entity40.class,
			Entity41.class, Entity42.class, Entity43.class, Entity44.class,
			Entity45.class, Entity46.class, Entity47.class, Entity48.class,
			Entity49.class, Entity50.class, Entity51.class, Entity52.class,
			Entity53.class, Entity54.class, Entity55.class, Entity56.class,
			Entity57.class, Entity58.class, Entity59.class, Entity60.class,
			Entity61.class, Entity62.class, Entity63.class, Entity64.class
	};

	private StandardServiceRegistry serviceRegistry;

	@Setup
	public void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder()
				.clearSettings()
				.applySetting( AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect" )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false )
				.build();
	}

	@TearDown
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Benchmark
	public void buildMetadata(Blackhole blackhole) {
		final var metadataSources = new MetadataSources( serviceRegistry );
		for ( Class<?> entityClass : ENTITY_CLASSES ) {
			metadataSources.addAnnotatedClass( entityClass );
		}
		blackhole.consume( metadataSources.buildMetadata() );
	}

	@MappedSuperclass
	public abstract static class BenchmarkEntity {
		@Id
		private Long id;
	}

	@Entity public static class Entity01 extends BenchmarkEntity {}
	@Entity public static class Entity02 extends BenchmarkEntity {}
	@Entity public static class Entity03 extends BenchmarkEntity {}
	@Entity public static class Entity04 extends BenchmarkEntity {}
	@Entity public static class Entity05 extends BenchmarkEntity {}
	@Entity public static class Entity06 extends BenchmarkEntity {}
	@Entity public static class Entity07 extends BenchmarkEntity {}
	@Entity public static class Entity08 extends BenchmarkEntity {}
	@Entity public static class Entity09 extends BenchmarkEntity {}
	@Entity public static class Entity10 extends BenchmarkEntity {}
	@Entity public static class Entity11 extends BenchmarkEntity {}
	@Entity public static class Entity12 extends BenchmarkEntity {}
	@Entity public static class Entity13 extends BenchmarkEntity {}
	@Entity public static class Entity14 extends BenchmarkEntity {}
	@Entity public static class Entity15 extends BenchmarkEntity {}
	@Entity public static class Entity16 extends BenchmarkEntity {}
	@Entity public static class Entity17 extends BenchmarkEntity {}
	@Entity public static class Entity18 extends BenchmarkEntity {}
	@Entity public static class Entity19 extends BenchmarkEntity {}
	@Entity public static class Entity20 extends BenchmarkEntity {}
	@Entity public static class Entity21 extends BenchmarkEntity {}
	@Entity public static class Entity22 extends BenchmarkEntity {}
	@Entity public static class Entity23 extends BenchmarkEntity {}
	@Entity public static class Entity24 extends BenchmarkEntity {}
	@Entity public static class Entity25 extends BenchmarkEntity {}
	@Entity public static class Entity26 extends BenchmarkEntity {}
	@Entity public static class Entity27 extends BenchmarkEntity {}
	@Entity public static class Entity28 extends BenchmarkEntity {}
	@Entity public static class Entity29 extends BenchmarkEntity {}
	@Entity public static class Entity30 extends BenchmarkEntity {}
	@Entity public static class Entity31 extends BenchmarkEntity {}
	@Entity public static class Entity32 extends BenchmarkEntity {}
	@Entity public static class Entity33 extends BenchmarkEntity {}
	@Entity public static class Entity34 extends BenchmarkEntity {}
	@Entity public static class Entity35 extends BenchmarkEntity {}
	@Entity public static class Entity36 extends BenchmarkEntity {}
	@Entity public static class Entity37 extends BenchmarkEntity {}
	@Entity public static class Entity38 extends BenchmarkEntity {}
	@Entity public static class Entity39 extends BenchmarkEntity {}
	@Entity public static class Entity40 extends BenchmarkEntity {}
	@Entity public static class Entity41 extends BenchmarkEntity {}
	@Entity public static class Entity42 extends BenchmarkEntity {}
	@Entity public static class Entity43 extends BenchmarkEntity {}
	@Entity public static class Entity44 extends BenchmarkEntity {}
	@Entity public static class Entity45 extends BenchmarkEntity {}
	@Entity public static class Entity46 extends BenchmarkEntity {}
	@Entity public static class Entity47 extends BenchmarkEntity {}
	@Entity public static class Entity48 extends BenchmarkEntity {}
	@Entity public static class Entity49 extends BenchmarkEntity {}
	@Entity public static class Entity50 extends BenchmarkEntity {}
	@Entity public static class Entity51 extends BenchmarkEntity {}
	@Entity public static class Entity52 extends BenchmarkEntity {}
	@Entity public static class Entity53 extends BenchmarkEntity {}
	@Entity public static class Entity54 extends BenchmarkEntity {}
	@Entity public static class Entity55 extends BenchmarkEntity {}
	@Entity public static class Entity56 extends BenchmarkEntity {}
	@Entity public static class Entity57 extends BenchmarkEntity {}
	@Entity public static class Entity58 extends BenchmarkEntity {}
	@Entity public static class Entity59 extends BenchmarkEntity {}
	@Entity public static class Entity60 extends BenchmarkEntity {}
	@Entity public static class Entity61 extends BenchmarkEntity {}
	@Entity public static class Entity62 extends BenchmarkEntity {}
	@Entity public static class Entity63 extends BenchmarkEntity {}
	@Entity public static class Entity64 extends BenchmarkEntity {}
}
