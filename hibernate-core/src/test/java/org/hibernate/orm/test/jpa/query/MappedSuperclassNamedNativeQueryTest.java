package org.hibernate.orm.test.jpa.query;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Named native queries declared on an abstract mapped superclass must be registered.
///
/// @author Steve Ebersole
@JiraKey( "HHH-14572" )
@Jpa( annotatedClasses = MappedSuperclassNamedNativeQueryTest.Record.class )
public class MappedSuperclassNamedNativeQueryTest {
	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testInheritedNamedNativeQueries(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Record first = new Record();
			first.id = 1L;
			first.description = "first";
			entityManager.persist( first );
			final Record second = new Record();
			second.id = 2L;
			second.description = "second";
			entityManager.persist( second );
		} );

		scope.inEntityManager( entityManager -> {
			final List<Record> records = entityManager.createNamedQuery( "MappedRecord.all", Record.class ).getResultList();
			assertEquals( List.of( 1L, 2L ), records.stream().map( record -> record.id ).toList() );
			assertEquals( List.of( "first", "second" ), records.stream().map( record -> record.description ).toList() );
			final Record second = entityManager.createNamedQuery( "MappedRecord.byId", Record.class )
					.setParameter( 1, 2L ).getSingleResult();
			assertEquals( 2L, second.id );
			assertEquals( "second", second.description );
		} );
	}

	@MappedSuperclass
	@NamedNativeQueries( {
			@NamedNativeQuery( name = "MappedRecord.all", query = "select * from hhh14572_record order by id", resultClass = Record.class ),
			@NamedNativeQuery( name = "MappedRecord.byId", query = "select * from hhh14572_record where id = ?", resultClass = Record.class )
	} )
	public abstract static class BaseRecord {
		@Id
		protected Long id;
	}

	@Entity( name = "MappedRecord" )
	@Table( name = "hhh14572_record" )
	public static class Record extends BaseRecord {
		private String description;
	}
}
