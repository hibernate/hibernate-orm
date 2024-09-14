/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.domain.animal.Animal;
import org.hibernate.testing.orm.domain.animal.Human;
import org.hibernate.testing.orm.domain.animal.Name;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hibernate.internal.util.collections.CollectionHelper.toMap;
import static org.junit.Assert.assertThat;

/**
 * Isolated test for various usages of parameters
 *
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.ANIMAL )
@SessionFactory
public class LegacyParameterTests {
	@Test
	@JiraKey( value = "HHH-9154" )
	public void testClassAsParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					s.createQuery( "from Human h where h.name = :class" ).setParameter( "class", new Name() ).list();
					s.createQuery( "from Human where name = :class" ).setParameter( "class", new Name() ).list();
					s.createQuery( "from Human h where :class = h.name" ).setParameter( "class", new Name() ).list();
					s.createQuery( "from Human h where :class <> h.name" ).setParameter( "class", new Name() ).list();
				}
		);
	}


	@Test
	@JiraKey(value = "HHH-7705")
	public void testSetPropertiesMapWithNullValues(SessionFactoryScope scope) {

		scope.inTransaction(
				(s) -> {
					Map<String,String> parameters = toMap( "nickName", null );

					Query<Human> q = s.createQuery(
							"from Human h where h.nickName = :nickName or (h.nickName is null and :nickName is null)",
							Human.class
					);
					q.setProperties( (parameters) );
					assertThat( q.list().size(), is( 0 ) );

					Human human1 = new Human();
					human1.setId( 2L );
					human1.setNickName( null );
					s.persist( human1 );

					parameters = new HashMap<>();

					parameters.put( "nickName", null );
					q = s.createQuery( "from Human h where h.nickName = :nickName or (h.nickName is null and :nickName is null)", Human.class );
					q.setProperties( (parameters) );
					assertThat( q.list().size(), is( 1 ) );
					Human found = q.list().get( 0 );
					assertThat( found.getId(), is( human1.getId() ) );

					parameters = new HashMap<>();
					parameters.put( "nickName", "nick" );

					q = s.createQuery( "from Human h where h.nickName = :nickName or (h.nickName is null and :nickName is null)", Human.class );
					q.setProperties( (parameters) );
					assertThat( q.list().size(), is( 1 ) );
					found = q.list().get( 0 );
					assertThat( found.getId(), is( 1L ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10796")
	public void testSetPropertiesMapNotContainingAllTheParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					Map<String,String> parameters = toMap( "nickNames", "nick" );

					List<Integer> intValues = new ArrayList<>();
					intValues.add( 1 );
					//noinspection unchecked
					Query<Human> q = s.createQuery(
							"from Human h where h.nickName in (:nickNames) and h.intValue in (:intValues)"
					);
					q.setParameterList( "intValues" , intValues);
					q.setProperties( (parameters) );
					assertThat( q.list().size(), is( 1 ) );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9154" )
	public void testObjectAsParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					s.createQuery( "from Human h where h.name = :OBJECT" ).setParameter( "OBJECT", new Name() ).list();
					s.createQuery( "from Human where name = :OBJECT" ).setParameter( "OBJECT", new Name() ).list();
					s.createQuery( "from Human h where :OBJECT = h.name" ).setParameter( "OBJECT", new Name() ).list();
					s.createQuery( "from Human h where :OBJECT <> h.name" ).setParameter( "OBJECT", new Name() ).list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-13310")
	public void testGetParameterListValue(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Query<Animal> query = session.createQuery( "from Animal a where a.id in :ids", Animal.class );
					query.setParameterList( "ids", Arrays.asList( 1L, 2L ) );

					Object parameterListValue = query.getParameterValue( "ids" );
					assertThat( parameterListValue, is( Arrays.asList( 1L, 2L ) ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-13310")
	public void testGetParameterListValueAfterParameterExpansion(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Query<Animal> query = session.createQuery( "from Animal a where a.id in :ids", Animal.class );
					query.setParameterList( "ids", Arrays.asList( 1L, 2L ) );
					query.list();

					Object parameterListValue = query.getParameterValue( "ids" );
					assertThat( parameterListValue, is( Arrays.asList( 1L, 2L ) ) );
				}
		);
	}

	@Test
	@ExpectedException( IllegalStateException.class )
	@JiraKey(value = "HHH-13310")
	public void testGetNotBoundParameterListValue(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Query<Animal> query = session.createQuery( "from Animal a where a.id in :ids", Animal.class );
					query.getParameterValue( "ids" );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-13310")
	public void testGetPositionalParameterListValue(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Query<Animal> query = session.createQuery( "from Animal a where a.id in ?1", Animal.class );
					query.setParameter( 1, Arrays.asList( 1L, 2L ) );

					Object parameterListValue = query.getParameterValue( 1 );
					assertThat( parameterListValue, is( Arrays.asList( 1L, 2L ) ) );

					query.list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-13310")
	public void testGetPositionalParameterValue(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query<Animal> query = session.createQuery( "from Animal a where a.id = ?1", Animal.class );
					query.setParameter( 1,  1L  );

					Object parameterListValue = query.getParameterValue( 1 );
					assertThat( parameterListValue, is( 1L ) );

					query.list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-13310")
	public void testGetParameterByPositionListValueAfterParameterExpansion(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Query<Animal> query = session.createQuery( "from Animal a where a.id in ?1", Animal.class );
					query.setParameterList( 1, Arrays.asList( 1L, 2L ) );
					query.list();

					Object parameterListValue = query.getParameterValue( 1 );
					assertThat( parameterListValue, is( Arrays.asList( 1L, 2L ) ) );
				}
		);
	}

	@Test
	@ExpectedException( IllegalStateException.class )
	@JiraKey(value = "HHH-13310")
	public void testGetPositionalNotBoundParameterListValue(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Query<Animal> query = session.createQuery( "from Animal a where a.id in ?1", Animal.class );
					query.getParameterValue( 1 );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					Human human = new Human();
					human.setId( 1L );
					human.setNickName( "nick" );
					human.setIntValue( 1 );
					s.persist( human );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					s.createQuery( "delete Human" ).executeUpdate();
				}
		);
	}
}
