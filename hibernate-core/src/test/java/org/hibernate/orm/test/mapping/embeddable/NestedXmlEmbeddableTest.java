/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.MutableValue;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlAggregate.class)
@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 23, versionMatchMode = VersionMatchMode.SAME_OR_NEWER,
		reason = "Currently failing on Oracle 23+ due to Bug 37319693 - ORA-00600 with check constraint on xml type")
public class NestedXmlEmbeddableTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			XmlHolder.class
		};
	}

	@BeforeEach
	public void setUp() {
		inTransaction(
				session -> {
					session.persist( new XmlHolder( 1L, "XYZ", 10, "String \"<abc>A&B</abc>\"", EmbeddableAggregate.createAggregate1() ) );
					session.persist( new XmlHolder( 2L, null, 20, "String 'abc'", EmbeddableAggregate.createAggregate2() ) );
				}
		);
	}

	@AfterEach
	protected void cleanupTest() {
		sessionFactoryScope().getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					XmlHolder XmlHolder = entityManager.find( XmlHolder.class, 1L );
					XmlHolder.setAggregate( EmbeddableAggregate.createAggregate2() );
					entityManager.flush();
					entityManager.clear();
					XmlHolder = entityManager.find( XmlHolder.class, 1L );
					assertEquals( "XYZ", XmlHolder.theXml.stringField );
					assertEquals( 10, XmlHolder.theXml.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableAggregate.createAggregate2(), XmlHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testFetch() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<XmlHolder> XmlHolders = entityManager.createQuery( "from XmlHolder b where b.id = 1", XmlHolder.class ).getResultList();
					assertEquals( 1, XmlHolders.size() );
					XmlHolder XmlHolder = XmlHolders.get( 0 );
					assertEquals( 1L, XmlHolder.getId() );
					assertEquals( "XYZ", XmlHolder.theXml.stringField );
					assertEquals( 10, XmlHolder.theXml.simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", XmlHolder.theXml.simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), XmlHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testFetchNull() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<XmlHolder> XmlHolders = entityManager.createQuery( "from XmlHolder b where b.id = 2", XmlHolder.class ).getResultList();
					assertEquals( 1, XmlHolders.size() );
					XmlHolder XmlHolder = XmlHolders.get( 0 );
					assertEquals( 2L, XmlHolder.getId() );
					assertNull( XmlHolder.theXml.stringField );
					assertEquals( 20, XmlHolder.theXml.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableAggregate.createAggregate2(), XmlHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testDomainResult() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<TheXml> structs = entityManager.createQuery( "select b.theXml from XmlHolder b where b.id = 1", TheXml.class ).getResultList();
					assertEquals( 1, structs.size() );
					TheXml theXml = structs.get( 0 );
					assertEquals( "XYZ", theXml.stringField );
					assertEquals( 10, theXml.simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", theXml.simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), theXml.nested );
				}
		);
	}

	@Test
	public void testSelectionItems() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<Tuple> tuples = entityManager.createQuery(
							"select " +
									"b.theXml.nested.theInt," +
									"b.theXml.nested.theDouble," +
									"b.theXml.nested.theBoolean," +
									"b.theXml.nested.theNumericBoolean," +
									"b.theXml.nested.theStringBoolean," +
									"b.theXml.nested.theString," +
									"b.theXml.nested.theInteger," +
									"b.theXml.nested.theUrl," +
									"b.theXml.nested.theClob," +
									"b.theXml.nested.theBinary," +
									"b.theXml.nested.theDate," +
									"b.theXml.nested.theTime," +
									"b.theXml.nested.theTimestamp," +
									"b.theXml.nested.theInstant," +
									"b.theXml.nested.theUuid," +
									"b.theXml.nested.gender," +
									"b.theXml.nested.convertedGender," +
									"b.theXml.nested.ordinalGender," +
									"b.theXml.nested.theDuration," +
									"b.theXml.nested.theLocalDateTime," +
									"b.theXml.nested.theLocalDate," +
									"b.theXml.nested.theLocalTime," +
									"b.theXml.nested.theZonedDateTime," +
									"b.theXml.nested.theOffsetDateTime," +
									"b.theXml.nested.mutableValue," +
									"b.theXml.simpleEmbeddable," +
									"b.theXml.simpleEmbeddable.doubleNested," +
									"b.theXml.simpleEmbeddable.doubleNested.theNested," +
									"b.theXml.simpleEmbeddable.doubleNested.theNested.theLeaf " +
									"from XmlHolder b where b.id = 1",
							Tuple.class
					).getResultList();
					assertEquals( 1, tuples.size() );
					final Tuple tuple = tuples.get( 0 );
					final EmbeddableAggregate struct = new EmbeddableAggregate();
					struct.setTheInt( tuple.get( 0, int.class ) );
					struct.setTheDouble( tuple.get( 1, Double.class ) );
					struct.setTheBoolean( tuple.get( 2, Boolean.class ) );
					struct.setTheNumericBoolean( tuple.get( 3, Boolean.class ) );
					struct.setTheStringBoolean( tuple.get( 4, Boolean.class ) );
					struct.setTheString( tuple.get( 5, String.class ) );
					struct.setTheInteger( tuple.get( 6, Integer.class ) );
					struct.setTheUrl( tuple.get( 7, URL.class ) );
					struct.setTheClob( tuple.get( 8, String.class ) );
					struct.setTheBinary( tuple.get( 9, byte[].class ) );
					struct.setTheDate( tuple.get( 10, Date.class ) );
					struct.setTheTime( tuple.get( 11, Time.class ) );
					struct.setTheTimestamp( tuple.get( 12, Timestamp.class ) );
					struct.setTheInstant( tuple.get( 13, Instant.class ) );
					struct.setTheUuid( tuple.get( 14, UUID.class ) );
					struct.setGender( tuple.get( 15, EntityOfBasics.Gender.class ) );
					struct.setConvertedGender( tuple.get( 16, EntityOfBasics.Gender.class ) );
					struct.setOrdinalGender( tuple.get( 17, EntityOfBasics.Gender.class ) );
					struct.setTheDuration( tuple.get( 18, Duration.class ) );
					struct.setTheLocalDateTime( tuple.get( 19, LocalDateTime.class ) );
					struct.setTheLocalDate( tuple.get( 20, LocalDate.class ) );
					struct.setTheLocalTime( tuple.get( 21, LocalTime.class ) );
					struct.setTheZonedDateTime( tuple.get( 22, ZonedDateTime.class ) );
					struct.setTheOffsetDateTime( tuple.get( 23, OffsetDateTime.class ) );
					struct.setMutableValue( tuple.get( 24, MutableValue.class ) );
					EmbeddableAggregate.assertEquals( EmbeddableAggregate.createAggregate1(), struct );

					SimpleEmbeddable simpleEmbeddable = tuple.get( 25, SimpleEmbeddable.class );
					assertEquals( simpleEmbeddable.doubleNested, tuple.get( 26, DoubleNested.class ) );
					assertEquals( simpleEmbeddable.doubleNested.theNested, tuple.get( 27, Nested.class ) );
					assertEquals( simpleEmbeddable.doubleNested.theNested.theLeaf, tuple.get( 28, Leaf.class ) );
					assertEquals( 10, simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
				}
		);
	}

	@Test
	public void testDeleteWhere() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "delete XmlHolder b where b.theXml is not null" ).executeUpdate();
					assertNull( entityManager.find( XmlHolder.class, 1L ) );

				}
		);
	}

	@Test
	public void testUpdateAggregate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update XmlHolder b set b.theXml = null" ).executeUpdate();
					assertNull( entityManager.find( XmlHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17695" )
	public void testNullNestedAggregate() {
		sessionFactoryScope().inTransaction(
			entityManager -> {
				XmlHolder XmlHolder = new XmlHolder(3L, "abc", 30, "String 'xyz'", null );
				entityManager.persist( XmlHolder );
			}
		);
		sessionFactoryScope().inTransaction(
				entityManager -> {
					XmlHolder XmlHolder = entityManager.createQuery( "from XmlHolder b where b.id = 3", XmlHolder.class ).getSingleResult();
					assertEquals( "abc", XmlHolder.theXml.stringField );
					assertEquals( 30, XmlHolder.theXml.simpleEmbeddable.integerField );
					assertEquals( "String 'xyz'", XmlHolder.theXml.simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
					assertNull( XmlHolder.getAggregate() );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17695" )
	public void testNullNestedEmbeddable() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					XmlHolder XmlHolder = new XmlHolder( );
					XmlHolder.setId( 3L );
					XmlHolder.setTheXml( new TheXml( "abc", null, EmbeddableAggregate.createAggregate1() ) );
					entityManager.persist( XmlHolder );
				}
		);
		sessionFactoryScope().inTransaction(
				entityManager -> {
					XmlHolder XmlHolder = entityManager.createQuery( "from XmlHolder b where b.id = 3", XmlHolder.class ).getSingleResult();
					assertEquals( "abc", XmlHolder.theXml.stringField );
					assertNull( XmlHolder.theXml.simpleEmbeddable );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), XmlHolder.getAggregate() );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17695" )
	public void testNullNestedEmbeddableAndAggregate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					XmlHolder XmlHolder = new XmlHolder( );
					XmlHolder.setId( 3L );
					XmlHolder.setTheXml( new TheXml( "abc", null, null ) );
					entityManager.persist( XmlHolder );
				}
		);
		sessionFactoryScope().inTransaction(
				entityManager -> {
					XmlHolder XmlHolder = entityManager.createQuery( "from XmlHolder b where b.id = 3", XmlHolder.class ).getSingleResult();
					assertEquals( "abc", XmlHolder.theXml.stringField );
					assertNull( XmlHolder.theXml.simpleEmbeddable );
					assertNull( XmlHolder.getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlComponentUpdate.class)
	public void testUpdateAggregateMember() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update XmlHolder b set b.theXml.nested.theString = null" ).executeUpdate();
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					struct.setTheString( null );
					assertStructEquals( struct, entityManager.find( XmlHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlComponentUpdate.class)
	public void testUpdateAggregateMemberOnNestedNull() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update XmlHolder b set b.theXml.simpleEmbeddable.doubleNested = null" ).executeUpdate();
					entityManager.createMutationQuery( "update XmlHolder b set b.theXml.simpleEmbeddable.doubleNested.theNested.theLeaf.stringField = 'Abc'" ).executeUpdate();
					assertEquals( "Abc", entityManager.find( XmlHolder.class, 1L ).getTheXml().simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlComponentUpdate.class)
	public void testUpdateMultipleAggregateMembers() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update XmlHolder b set b.theXml.nested.theString = null, b.theXml.nested.theUuid = null" ).executeUpdate();
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					struct.setTheString( null );
					struct.setTheUuid( null );
					assertStructEquals( struct, entityManager.find( XmlHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlComponentUpdate.class)
	public void testUpdateAllAggregateMembers() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					entityManager.createMutationQuery(
							"update XmlHolder b set " +
									"b.theXml.nested.theInt = :theInt," +
									"b.theXml.nested.theDouble = :theDouble," +
									"b.theXml.nested.theBoolean = :theBoolean," +
									"b.theXml.nested.theNumericBoolean = :theNumericBoolean," +
									"b.theXml.nested.theStringBoolean = :theStringBoolean," +
									"b.theXml.nested.theString = :theString," +
									"b.theXml.nested.theInteger = :theInteger," +
									"b.theXml.nested.theUrl = :theUrl," +
									"b.theXml.nested.theClob = :theClob," +
									"b.theXml.nested.theBinary = :theBinary," +
									"b.theXml.nested.theDate = :theDate," +
									"b.theXml.nested.theTime = :theTime," +
									"b.theXml.nested.theTimestamp = :theTimestamp," +
									"b.theXml.nested.theInstant = :theInstant," +
									"b.theXml.nested.theUuid = :theUuid," +
									"b.theXml.nested.gender = :gender," +
									"b.theXml.nested.convertedGender = :convertedGender," +
									"b.theXml.nested.ordinalGender = :ordinalGender," +
									"b.theXml.nested.theDuration = :theDuration," +
									"b.theXml.nested.theLocalDateTime = :theLocalDateTime," +
									"b.theXml.nested.theLocalDate = :theLocalDate," +
									"b.theXml.nested.theLocalTime = :theLocalTime," +
									"b.theXml.nested.theZonedDateTime = :theZonedDateTime," +
									"b.theXml.nested.theOffsetDateTime = :theOffsetDateTime," +
									"b.theXml.nested.mutableValue = :mutableValue," +
									"b.theXml.simpleEmbeddable.integerField = :integerField " +
									"where b.id = 2"
					)
							.setParameter( "theInt", struct.getTheInt() )
							.setParameter( "theDouble", struct.getTheDouble() )
							.setParameter( "theBoolean", struct.isTheBoolean() )
							.setParameter( "theNumericBoolean", struct.isTheNumericBoolean() )
							.setParameter( "theStringBoolean", struct.isTheStringBoolean() )
							.setParameter( "theString", struct.getTheString() )
							.setParameter( "theInteger", struct.getTheInteger() )
							.setParameter( "theUrl", struct.getTheUrl() )
							.setParameter( "theClob", struct.getTheClob() )
							.setParameter( "theBinary", struct.getTheBinary() )
							.setParameter( "theDate", struct.getTheDate() )
							.setParameter( "theTime", struct.getTheTime() )
							.setParameter( "theTimestamp", struct.getTheTimestamp() )
							.setParameter( "theInstant", struct.getTheInstant() )
							.setParameter( "theUuid", struct.getTheUuid() )
							.setParameter( "gender", struct.getGender() )
							.setParameter( "convertedGender", struct.getConvertedGender() )
							.setParameter( "ordinalGender", struct.getOrdinalGender() )
							.setParameter( "theDuration", struct.getTheDuration() )
							.setParameter( "theLocalDateTime", struct.getTheLocalDateTime() )
							.setParameter( "theLocalDate", struct.getTheLocalDate() )
							.setParameter( "theLocalTime", struct.getTheLocalTime() )
							.setParameter( "theZonedDateTime", struct.getTheZonedDateTime() )
							.setParameter( "theOffsetDateTime", struct.getTheOffsetDateTime() )
							.setParameter( "mutableValue", struct.getMutableValue() )
							.setParameter( "integerField", 5 )
							.executeUpdate();
					XmlHolder XmlHolder = entityManager.find( XmlHolder.class, 2L );
					assertEquals( 5, XmlHolder.theXml.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), XmlHolder.getAggregate() );
				}
		);
	}

	private static void assertStructEquals(EmbeddableAggregate struct, EmbeddableAggregate struct2) {
		assertArrayEquals( struct.getTheBinary(), struct2.getTheBinary() );
		assertEquals( struct.getTheString(), struct2.getTheString() );
		assertEquals( struct.getTheLocalDateTime(), struct2.getTheLocalDateTime() );
		assertEquals( struct.getTheUuid(), struct2.getTheUuid() );
	}

	@Entity(name = "XmlHolder")
	public static class XmlHolder {
		@Id
		private Long id;
		@JdbcTypeCode(SqlTypes.SQLXML)
		private TheXml theXml;

		public XmlHolder() {
		}

		public XmlHolder(Long id, String stringField, Integer integerField, String leaf, EmbeddableAggregate aggregate) {
			this.id = id;
			this.theXml = new TheXml( stringField, integerField, leaf, aggregate );
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public TheXml getTheXml() {
			return theXml;
		}

		public void setTheXml(TheXml struct) {
			this.theXml = struct;
		}

		public EmbeddableAggregate getAggregate() {
			return theXml == null ? null : theXml.nested;
		}

		public void setAggregate(EmbeddableAggregate aggregate) {
			if ( theXml == null ) {
				theXml = new TheXml( null, null, null, aggregate );
			}
			else {
				theXml.nested = aggregate;
			}
		}

	}

	@Embeddable
	public static class TheXml {
		private String stringField;
		private SimpleEmbeddable simpleEmbeddable;
		@JdbcTypeCode(SqlTypes.SQLXML)
		private EmbeddableAggregate nested;

		public TheXml() {
		}

		public TheXml(String stringField, Integer integerField, String leaf, EmbeddableAggregate nested) {
			this.stringField = stringField;
			this.simpleEmbeddable = new SimpleEmbeddable( integerField, leaf );
			this.nested = nested;
		}

		public TheXml(String stringField, SimpleEmbeddable simpleEmbeddable, EmbeddableAggregate nested) {
			this.stringField = stringField;
			this.simpleEmbeddable = simpleEmbeddable;
			this.nested = nested;
		}
	}

	@Embeddable
	public static class SimpleEmbeddable {
		private Integer integerField;
		@JdbcTypeCode(SqlTypes.SQLXML)
		private DoubleNested doubleNested;

		public SimpleEmbeddable() {
		}

		public SimpleEmbeddable(Integer integerField, String leaf) {
			this.integerField = integerField;
			this.doubleNested = new DoubleNested( leaf );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			SimpleEmbeddable that = (SimpleEmbeddable) o;

			if ( !Objects.equals( integerField, that.integerField ) ) {
				return false;
			}
			return Objects.equals( doubleNested, that.doubleNested );
		}

		@Override
		public int hashCode() {
			int result = integerField != null ? integerField.hashCode() : 0;
			result = 31 * result + ( doubleNested != null ? doubleNested.hashCode() : 0 );
			return result;
		}
	}

	@Embeddable
	public static class DoubleNested {
		@JdbcTypeCode(SqlTypes.SQLXML)
		private Nested theNested;

		public DoubleNested() {
		}

		public DoubleNested(String leaf) {
			this.theNested = new Nested( leaf );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			DoubleNested that = (DoubleNested) o;

			return Objects.equals( theNested, that.theNested );
		}

		@Override
		public int hashCode() {
			return theNested != null ? theNested.hashCode() : 0;
		}
	}

	@Embeddable
	public static class Nested {
		@JdbcTypeCode(SqlTypes.SQLXML)
		private Leaf theLeaf;

		public Nested() {
		}

		public Nested(String stringField) {
			this.theLeaf = new Leaf( stringField );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Nested nested = (Nested) o;

			return Objects.equals( theLeaf, nested.theLeaf );
		}

		@Override
		public int hashCode() {
			return theLeaf != null ? theLeaf.hashCode() : 0;
		}
	}

	@Embeddable
	public static class Leaf {
		private String stringField;

		public Leaf() {
		}

		public Leaf(String stringField) {
			this.stringField = stringField;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Leaf leaf = (Leaf) o;

			return Objects.equals( stringField, leaf.stringField );
		}

		@Override
		public int hashCode() {
			return stringField != null ? stringField.hashCode() : 0;
		}
	}
}
