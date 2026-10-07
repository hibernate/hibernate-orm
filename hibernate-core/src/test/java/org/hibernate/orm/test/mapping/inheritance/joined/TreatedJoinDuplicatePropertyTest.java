package org.hibernate.orm.test.mapping.inheritance.joined;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.Join;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Regression for treated association joins whose sibling targets declare the same property.
///
/// @author Steve Ebersole
@JiraKey("HHH-10759")
@DomainModel(annotatedClasses = {
		TreatedJoinDuplicatePropertyTest.RefEntity.class,
		TreatedJoinDuplicatePropertyTest.BaseType.class,
		TreatedJoinDuplicatePropertyTest.SubType1.class,
		TreatedJoinDuplicatePropertyTest.SubType2.class
})
@SessionFactory
public class TreatedJoinDuplicatePropertyTest {
	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var first = new SubType1();
			first.id = 1L;
			first.myvalue = "match";
			var second = new SubType2();
			second.id = 2L;
			second.myvalue = "match";
			var otherFirst = new SubType1();
			otherFirst.id = 3L;
			otherFirst.myvalue = "other";
			var otherSecond = new SubType2();
			otherSecond.id = 4L;
			otherSecond.myvalue = "other";
			for ( BaseType target : new BaseType[] { first, second, otherFirst, otherSecond } ) {
				session.persist( target );
				var reference = new RefEntity();
				reference.id = target.id;
				reference.other = target;
				session.persist( reference );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void criteriaTreatedJoin(boolean firstSubtype, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var builder = session.getCriteriaBuilder();
			var query = builder.createQuery( RefEntity.class );
			var root = query.from( RefEntity.class );
			Join<RefEntity, BaseType> join = root.join( "other" );
			var treated = firstSubtype ? builder.treat( join, SubType1.class )
					: builder.treat( join, SubType2.class );
			var value = builder.parameter( String.class, "value" );
			query.select( root ).where( builder.equal( treated.get( "myvalue" ), value ) );
			assertThat( session.createQuery( query ).setParameter( "value", "match" ).getResultList() )
					.extracting( reference -> reference.id )
					.containsExactly( firstSubtype ? 1L : 2L );
		} );
	}

	@Entity(name = "RefEntity")
	public static class RefEntity {
		@Id
		Long id;
		@ManyToOne
		BaseType other;
	}

	@Entity(name = "BaseType")
	@Inheritance(strategy = InheritanceType.JOINED)
	public abstract static class BaseType {
		@Id
		Long id;
	}

	@Entity(name = "SubType1")
	public static class SubType1 extends BaseType {
		String myvalue;
	}

	@Entity(name = "SubType2")
	public static class SubType2 extends BaseType {
		String myvalue;
	}
}
