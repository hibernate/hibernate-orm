package org.hibernate.orm.test.function.json;

import jakarta.persistence.Tuple;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.function.json.JsonTestHelper.assertNoInjection;

/**
 * @author Christian Beikov
 */
@DomainModel
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsJsonObject.class)
public class JsonObjectTest {

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-object-example[]
			em.createQuery( "select json_object('key', 'value'), json_object(KEY 'key1' VALUE 'value1', 'key2' VALUE 'value2', 'key3': 'value3')" ).getResultList();
			//end::hql-json-object-example[]
		} );
	}

	@Test
	public void testNullClause(SessionFactoryScope scope) {
		scope.inSession( em -> {
			em.createQuery("select json_object('key': null null on null)" ).getResultList();
		} );
	}

	@Test
	public void testAbsentOnNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-object-on-null-example[]
			em.createQuery("select json_object('key': null absent on null)" ).getResultList();
			//end::hql-json-object-on-null-example[]
		} );
	}

	@Test
	public void testKeyInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_object(:path: cast('{}' as json))", Tuple.class )
					.setParameter( "path", "'--" )
					.getResultList();
		} );
	}

	@Test
	public void testKeyBackslashInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_object(:path: cast('{}' as json))", Tuple.class )
					.setParameter( "path", "\\'--" )
					.getResultList();
		} );
	}

	@Test
	public void testKeyInjection2(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_object(:path: cast('{}' as json))", Tuple.class )
					.setParameter( "path", "\"--" )
					.getResultList();
		} );
	}

}
