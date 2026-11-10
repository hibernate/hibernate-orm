package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Formula;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-20494")
@RequiresDialect(MySQLDialect.class)
@DomainModel(annotatedClasses = InsertConflictFormulaTest.Account.class)
@SessionFactory
class InsertConflictFormulaTest {
	@Test
	void updateFromFormulaWithQuotedColumn(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var account = new Account();
			account.id = 1L;
			account.balance = 5;
			session.persist( account );
		} );

		scope.inTransaction( session -> {
			session.createMutationQuery( """
					insert into FormulaAccount (id, balance) values (1L, 100)
					on conflict(id) do update set balance = adjustedBalance
					""" ).executeUpdate();
			assertEquals( 6, session.find( Account.class, 1L ).balance );
		} );
	}

	@Entity(name = "FormulaAccount")
	static class Account {
		@Id
		long id;

		@Column(name = "`Balance`")
		int balance;

		@Formula("`Balance` + 1")
		int adjustedBalance;
	}
}
