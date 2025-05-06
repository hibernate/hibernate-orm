/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.io.Serializable;
import java.sql.Types;
import java.util.Locale;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class ExplicitJavaTypeDescriptorTest extends BaseNonConfigCoreFunctionalTestCase {

	private static int mutableToDatabaseCallCount;
	private static int mutableToDomainCallCount;

	private static int immutableToDatabaseCallCount;
	private static int immutableToDomainCallCount;

	private static int pseudoMutableToDatabaseCallCount;
	private static int pseudoMutableToDomainCallCount;

	@Test
	@JiraKey( value = "HHH-11098" )
	public void testIt() {
		// create data and check assertions
		inTransaction(
				(session) -> session.persist( new TheEntity( 1 ) )
		);

		// assertions based on the persist call
		assertThat( mutableToDomainCallCount, is(1 ) );  			// 1 instead of 0 because of the deep copy call
		assertThat( mutableToDatabaseCallCount, is(3 ) );  			// 2 instead of 1 because of the deep copy call

		assertThat( immutableToDomainCallCount, is(0 ) );			// logical
		assertThat( immutableToDatabaseCallCount, is(1 ) );			// logical

		assertThat( pseudoMutableToDomainCallCount, is(0 ) );		// was 1 (like mutable) before the JavaTypeDescriptor registration
		assertThat( pseudoMutableToDatabaseCallCount, is(1 ) );	// was 2 (like mutable) before the JavaTypeDescriptor registration
	}

	@Before
	public void clearCounts() {
		// in case we add additional tests
		sessionFactory().getStatistics().clear();

		mutableToDatabaseCallCount = 0;
		mutableToDomainCallCount = 0;

		immutableToDatabaseCallCount = 0;
		immutableToDomainCallCount = 0;

		pseudoMutableToDatabaseCallCount = 0;
		pseudoMutableToDomainCallCount = 0;
	}

	@After
	public void dropTestData() {
		inTransaction(
				session -> session.createQuery( "delete TheEntity" ).executeUpdate()
		);
	}

	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		( (TypeContributions) metadataBuilder ).contributeJavaType( PseudoMutableStateJavaType.INSTANCE );
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );

		// to make sure we get the deepCopy calls
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( TheEntity.class );
	}


	@Entity( name = "TheEntity")
	@Table( name = "T_ENTITY" )
	@Cacheable
	public static class TheEntity {
		@Id
		private Integer id;

		@Convert( converter = MutableConverterImpl.class )
		private MutableState mutableState;

		@Convert( converter = ImmutableConverterImpl.class )
		private ImmutableState immutableState;

		@Convert( converter = PseudoMutableConverterImpl.class )
		private PseudoMutableState immutableMutableState;

		public TheEntity() {
		}

		public TheEntity(Integer id) {
			this.id = id;

			this.mutableState = new MutableState( id.toString() );
			this.immutableState = new ImmutableState( id.toString() );
			this.immutableMutableState = new PseudoMutableState( id.toString() );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Purely mutable state

	public static class MutableState {
		private String state;

		public MutableState(String state) {
			this.state = state;
		}

		public String getState() {
			return state;
		}

		// mutable
		public void setState(String state) {
			this.state = state;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			MutableState that = (MutableState) o;

			return getState() != null ? getState().equals( that.getState() ) : that.getState() == null;

		}

		@Override
		public int hashCode() {
			return getState() != null ? getState().hashCode() : 0;
		}
	}

	@Converter
	public static class MutableConverterImpl implements AttributeConverter<MutableState,String> {
		@Override
		public String convertToDatabaseColumn(MutableState attribute) {
			mutableToDatabaseCallCount++;
			return attribute == null ? null : attribute.getState();
		}

		@Override
		public MutableState convertToEntityAttribute(String dbData) {
			mutableToDomainCallCount++;
			return new MutableState( dbData );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Purely immutable state

	@Immutable
	public static class ImmutableState implements Serializable {
		private final String state;

		public ImmutableState(String state) {
			this.state = state;
		}

		public String getState() {
			return state;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			ImmutableState that = (ImmutableState) o;

			return getState().equals( that.getState() );

		}

		@Override
		public int hashCode() {
			return getState().hashCode();
		}
	}

	@Converter
	public static class ImmutableConverterImpl implements AttributeConverter<ImmutableState,String> {
		@Override
		public String convertToDatabaseColumn(ImmutableState attribute) {
			immutableToDatabaseCallCount++;
			return attribute == null ? null : attribute.getState();
		}

		@Override
		public ImmutableState convertToEntityAttribute(String dbData) {
			immutableToDomainCallCount++;
			return new ImmutableState( dbData );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mutable state we treat as immutable

	public static class PseudoMutableState implements Serializable {
		private String state;

		public PseudoMutableState(String state) {
			this.state = state;
		}

		public String getState() {
			return state;
		}

		// mutable
		public void setState(String state) {
			// just a safety net - the idea is that the user is promising to not mutate the internal state
			throw new UnsupportedOperationException( "illegal attempt to mutate state" );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			PseudoMutableState that = (PseudoMutableState) o;

			return getState() != null ? getState().equals( that.getState() ) : that.getState() == null;

		}

		@Override
		public int hashCode() {
			return getState() != null ? getState().hashCode() : 0;
		}
	}

	@Converter
	public static class PseudoMutableConverterImpl implements AttributeConverter<PseudoMutableState,String> {
		@Override
		public String convertToDatabaseColumn(PseudoMutableState attribute) {
			pseudoMutableToDatabaseCallCount++;
			return attribute == null ? null : attribute.getState();
		}

		@Override
		public PseudoMutableState convertToEntityAttribute(String dbData) {
			pseudoMutableToDomainCallCount++;
			return new PseudoMutableState( dbData );
		}
	}

	public static class PseudoMutableStateJavaType implements JavaType<PseudoMutableState> {
		/**
		 * Singleton access
		 */
		public static final PseudoMutableStateJavaType INSTANCE = new PseudoMutableStateJavaType();

		@Override
		public Class<PseudoMutableState> getJavaTypeClass() {
			return PseudoMutableState.class;
		}

		@Override
		public MutabilityPlan<PseudoMutableState> getMutabilityPlan() {
			return ImmutableMutabilityPlan.instance();
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
			return context.getJdbcType( Types.VARCHAR );
		}

		@Override
		public PseudoMutableState fromString(CharSequence string) {
			return string == null ? null : new PseudoMutableState( string.toString() );
		}

		@Override
		public <X> X unwrap(PseudoMutableState value, Class<X> type, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}

			if ( PseudoMutableState.class.equals( type ) ) {
				return (X) value;
			}

			if ( String.class.equals( type ) ) {
				return (X) value.state;
			}

			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Cannot convert value '%s' to type `%s`",
							value.state,
							type
					)
			);
		}

		@Override
		public <X> PseudoMutableState wrap(X value, WrapperOptions options) {
			return null;
		}
	}


}
