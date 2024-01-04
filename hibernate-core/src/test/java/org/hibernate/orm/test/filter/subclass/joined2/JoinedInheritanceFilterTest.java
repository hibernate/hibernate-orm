package org.hibernate.orm.test.filter.subclass.joined2;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.assertj.core.util.TriFunction;

@TestForIssue(jiraKey = "HHH-9646")
@SessionFactory
@DomainModel(annotatedClasses = { Animal.class, Dog.class, Owner.class, JoinedInheritanceFilterTest.class })
@FilterDef(name = "companyFilter", parameters = @ParamDef(name = "companyIdParam", type = long.class))
public class JoinedInheritanceFilterTest implements SessionFactoryScopeAware {

	private SessionFactoryScope scope;

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void test(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction,
			TriFunction<SharedSessionContract, Class<?>, Object, Object> find) {
		inTransaction.accept( scope, s -> {
			s.createQuery( "SELECT o FROM Owner o INNER JOIN FETCH o.dog d WHERE o.id = 1" ).getResultList();
			s.enableFilter( "companyFilter" ).setParameter( "companyIdParam", 2l ).validate();
			s.createQuery( "SELECT o FROM Owner o INNER JOIN FETCH o.dog d WHERE o.id = 1" ).getResultList();
			s.createQuery( "FROM Animal" ).getResultList();
			s.createQuery( "FROM Dog" ).getResultList();
			assertNull( find.apply( s, Owner.class, 1 ) );
			assertNull( find.apply( s, Animal.class, 1 ) );
			assertNull( find.apply( s, Dog.class, 1 ) );
		} );
	}

	List<? extends Arguments> transactionKind() {
		// We want to test both regular and stateless session:
		BiConsumer<SessionFactoryScope, Consumer<SessionImplementor>> kind1 = SessionFactoryScope::inTransaction;
		TriFunction<Session, Class<?>, Object, Object> find1 = Session::get;
		BiConsumer<SessionFactoryScope, Consumer<StatelessSession>> kind2 = SessionFactoryScope::inStatelessTransaction;
		TriFunction<StatelessSession, Class<?>, Object, Object> find2 = StatelessSession::get;
		return List.of(
				Arguments.of( kind1, find1 ),
				Arguments.of( kind2, find2 )
		);
	}
}
