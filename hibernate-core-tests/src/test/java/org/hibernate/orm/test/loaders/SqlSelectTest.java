package org.hibernate.orm.test.loaders;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {SqlSelectTest.WithSqlSelect.class})
public class SqlSelectTest {

	@Test
	void test(SessionFactoryScope scope) {
		WithSqlSelect withSqlSelect = new WithSqlSelect();
		withSqlSelect.name = "Hibernate";
		withSqlSelect.uuids.add( SafeRandomUUIDGenerator.safeRandomUUID() );
		withSqlSelect.uuids.add( SafeRandomUUIDGenerator.safeRandomUUID() );
		withSqlSelect.uuids.add( SafeRandomUUIDGenerator.safeRandomUUID() );

		scope.inTransaction( s -> s.persist( withSqlSelect ) );

		scope.inSession( s -> {
			WithSqlSelect wss = s.get( WithSqlSelect.class, withSqlSelect.id );
			assertEquals( "Hibernate", wss.name );
			assertEquals( 3, wss.uuids.size() );
		});
	}

	@Entity
	@Table(name = "With_Sql_Select")
	@SQLSelect(sql = "select * from With_Sql_Select where sql_select_id = ?",
			querySpaces = "With_Sql_Select")
	static class WithSqlSelect {
		@Id @GeneratedValue
		@Column(name = "sql_select_id")
		Long id;
		String name;
		@ElementCollection
		@CollectionTable(name = "With_Uuids",
				joinColumns = @JoinColumn(name = "sql_select_id", referencedColumnName = "sql_select_id"))
		@SQLSelect(sql = "select Random_Uuids as uuid from With_Uuids where sql_select_id = ?",
				resultSetMapping = @SqlResultSetMapping(name = "",
						columns = @ColumnResult(name = "uuid", type = UUID.class)),
				querySpaces = "With_Uuids")
		@Column(name = "Random_Uuids")
		List<UUID> uuids = new ArrayList<>();
	}
}
