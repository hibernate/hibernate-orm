/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.query.Query;
import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.BasicType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Beikov
 */
@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(annotatedClasses = EnumSetConverterTest.TableWithEnumSetConverter.class)
@SessionFactory
public class EnumSetConverterTest {

	private BasicType<Set<MySpecialEnum>> enumSetType;

	@BeforeEach
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			//noinspection unchecked
			enumSetType = (BasicType<Set<MySpecialEnum>>) em.unwrap( SessionImplementor.class )
					.getFactory()
					.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( TableWithEnumSetConverter.class )
					.getPropertyType( "theSet" );
			em.persist( new TableWithEnumSetConverter( 1L, new HashSet<>() ) );
			em.persist( new TableWithEnumSetConverter( 2L, EnumSet.of( MySpecialEnum.VALUE1, MySpecialEnum.VALUE2 ) ) );
			em.persist( new TableWithEnumSetConverter( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithEnumSetConverter.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", EnumSet.of( MySpecialEnum.VALUE2, MySpecialEnum.VALUE1, MySpecialEnum.VALUE3 ), enumSetType );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_enum_set_convert(id, the_set) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", EnumSet.of( MySpecialEnum.VALUE2, MySpecialEnum.VALUE1, MySpecialEnum.VALUE3 ), enumSetType );
			q.executeUpdate();
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TableWithEnumSetConverter tableRecord;
			tableRecord = em.find( TableWithEnumSetConverter.class, 1L );
			assertThat( tableRecord.getTheSet(), is( new HashSet<>() ) );

			tableRecord = em.find( TableWithEnumSetConverter.class, 2L );
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MySpecialEnum.VALUE1, MySpecialEnum.VALUE2 ) ) );

			tableRecord = em.find( TableWithEnumSetConverter.class, 3L );
			assertThat( tableRecord.getTheSet(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithEnumSetConverter> tq = em.createNamedQuery( "TableWithEnumSetConverter.JPQL.getById", TableWithEnumSetConverter.class );
			tq.setParameter( "id", 2L );
			TableWithEnumSetConverter tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MySpecialEnum.VALUE1, MySpecialEnum.VALUE2 ) ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "The statement failed because binary large objects are not allowed in the Union, Intersect, or Minus ")
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 6,
			reason = "Bug in MariaDB https://jira.mariadb.org/browse/MDEV-21530")
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithEnumSetConverter> tq = em.createNamedQuery( "TableWithEnumSetConverter.JPQL.getByData", TableWithEnumSetConverter.class );
			tq.setParameter( "data", new HashSet<>() );
			TableWithEnumSetConverter tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithEnumSetConverter> tq = em.createNamedQuery( "TableWithEnumSetConverter.Native.getById", TableWithEnumSetConverter.class );
			tq.setParameter( "id", 2L );
			TableWithEnumSetConverter tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MySpecialEnum.VALUE1, MySpecialEnum.VALUE2 ) ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle requires a special function to compare XML")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 requires a special function to compare XML")
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "SQL Server requires a special function to compare XML")
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase ASE requires a special function to compare XML")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA requires a special function to compare LOBs")
	@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true, reason = "MySQL supports distinct from through a special operator")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix can't compare LOBs")
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final Dialect dialect = em.getDialect();
			final String op = dialect.supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			final String param = enumSetType.getJdbcType().wrapWriteExpression( ":data", null, dialect );
			Query<TableWithEnumSetConverter> tq = em.createNativeQuery(
					"SELECT * FROM table_with_enum_set_convert t WHERE the_set " + op + " " + param,
					TableWithEnumSetConverter.class
			);
			tq.setParameter( "data", EnumSet.of( MySpecialEnum.VALUE1, MySpecialEnum.VALUE2 ), enumSetType );
			TableWithEnumSetConverter tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Entity( name = "TableWithEnumSetConverter" )
	@Table( name = "table_with_enum_set_convert" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithEnumSetConverter.JPQL.getById",
				query = "SELECT t FROM TableWithEnumSetConverter t WHERE id = :id" ),
		@NamedQuery( name = "TableWithEnumSetConverter.JPQL.getByData",
				query = "SELECT t FROM TableWithEnumSetConverter t WHERE theSet IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithEnumSetConverter.Native.getById",
				query = "SELECT * FROM table_with_enum_set_convert t WHERE id = :id",
				resultClass = TableWithEnumSetConverter.class ),
		@NamedNativeQuery( name = "TableWithEnumSetConverter.Native.insert",
				query = "INSERT INTO table_with_enum_set_convert(id, the_set) VALUES ( :id , :data )" )
	} )
	public static class TableWithEnumSetConverter {

		@Id
		private Long id;

		@Convert(converter = MyEnumConverter.class)
		@Column( name = "the_set" )
		private Set<MySpecialEnum> theSet;

		public TableWithEnumSetConverter() {
		}

		public TableWithEnumSetConverter(Long id, Set<MySpecialEnum> theSet) {
			this.id = id;
			this.theSet = theSet;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<MySpecialEnum> getTheSet() {
			return theSet;
		}

		public void setTheSet(Set<MySpecialEnum> theSet) {
			this.theSet = theSet;
		}
	}

	public enum MySpecialEnum {
		VALUE1, VALUE2, VALUE3
	}

	public static class MyEnumConverter implements AttributeConverter<MySpecialEnum, String> {
		@Override
		public String convertToDatabaseColumn(MySpecialEnum attribute) {
			return attribute == null ? null : attribute.name();
		}

		@Override
		public MySpecialEnum convertToEntityAttribute(String dbData) {
			return dbData == null ? null : MySpecialEnum.valueOf( dbData );
		}
	}
}
