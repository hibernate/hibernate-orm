/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Cacheable;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.Immutable;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class ExplicitJavaTypeDescriptorTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureBootstrapServiceRegistryBuilder(BootstrapServiceRegistryBuilder bsrb) {
		super.configureBootstrapServiceRegistryBuilder( bsrb );

		// Let's tell Hibernate to treat MutableState2 as immutable
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor(
				new JavaTypeDescriptorRegistry.FallbackJavaTypeDescriptor( MutableState2.class ) {
					@Override
					public MutabilityPlan getMutabilityPlan() {
						return ImmutableMutabilityPlan.INSTANCE;
					}
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );

		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( TheEntity.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11098" )
	public void testIt() {
		// set up test data
		Session session = openSession();
		session.beginTransaction();
		session.persist( new TheEntity(1) );
		session.getTransaction().commit();
		session.close();

		// assertions based on the persist call
		assertThat( mutableToDomainCallCount, is(1) );  			// 1 instead of 0 because of the deep copy call
		assertThat( mutableToDatabaseCallCount, is(2) );  			// 2 instead of 1 because of the deep copy call

		assertThat( immutableToDomainCallCount, is(0) );			// logical
		assertThat( immutableToDatabaseCallCount, is(1) );			// logical

		assertThat( immutableMutableToDomainCallCount, is(0) );		// was 1 (like mutable) before the JavaTypeDescriptor registration
		assertThat( immutableMutableToDatabaseCallCount, is(1) );	// was 2 (like mutable) before the JavaTypeDescriptor registration

		// clean up test data
		session = openSession();
		session.beginTransaction();
		session.delete( session.byId( TheEntity.class ).getReference( 1 ) );
		session.getTransaction().commit();
		session.close();
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

		@Convert( converter = ImmutableMutable2ConverterImpl.class )
		private MutableState2 immutableMutableState;

		public TheEntity() {
		}

		public TheEntity(Integer id) {
			this.id = id;

			this.mutableState = new MutableState( id.toString() );
			this.immutableState = new ImmutableState( id.toString() );
			this.immutableMutableState = new MutableState2( id.toString() );
		}
	}

	@Before
	public void clearCounts() {
		// in case we add additional tests
		sessionFactory().getStatistics().clear();

		mutableToDatabaseCallCount = 0;
		mutableToDomainCallCount = 0;

		immutableToDatabaseCallCount = 0;
		immutableToDomainCallCount = 0;

		immutableMutableToDatabaseCallCount = 0;
		immutableMutableToDomainCallCount = 0;
	}

	private static int mutableToDatabaseCallCount;
	private static int mutableToDomainCallCount;

	private static int immutableToDatabaseCallCount;
	private static int immutableToDomainCallCount;

	private static int immutableMutableToDatabaseCallCount;
	private static int immutableMutableToDomainCallCount;


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
	public static class ImmutableState {
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

	public static class MutableState2 {
		private String state;

		public MutableState2(String state) {
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

			MutableState2 that = (MutableState2) o;

			return getState() != null ? getState().equals( that.getState() ) : that.getState() == null;

		}

		@Override
		public int hashCode() {
			return getState() != null ? getState().hashCode() : 0;
		}
	}

	@Converter
	public static class ImmutableMutable2ConverterImpl implements AttributeConverter<MutableState2,String> {
		@Override
		public String convertToDatabaseColumn(MutableState2 attribute) {
			immutableMutableToDatabaseCallCount++;
			return attribute == null ? null : attribute.getState();
		}

		@Override
		public MutableState2 convertToEntityAttribute(String dbData) {
			immutableMutableToDomainCallCount++;
			return new MutableState2( dbData );
		}
	}



}
