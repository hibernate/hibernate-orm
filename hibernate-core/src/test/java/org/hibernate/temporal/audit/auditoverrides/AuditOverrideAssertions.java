package org.hibernate.temporal.audit.auditoverrides;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.DomainModelScope;

import static org.assertj.core.api.Assertions.assertThat;

final class AuditOverrideAssertions {
	private AuditOverrideAssertions() {
	}

	static Table assertTable(DomainModelScope scope, String name, String... columns) {
		final var table = scope.getDomainModel().collectTableMappings().stream()
				.filter( candidate -> candidate.getName().equalsIgnoreCase( name ) )
				.findFirst().orElseThrow( () -> new AssertionError( "Missing table " + name ) );
		for ( var column : columns ) {
			assertThat( table.getColumn( new Column( column ) ) ).as( name + "." + column ).isNotNull();
		}
		return table;
	}

	static void assertNoTable(DomainModelScope scope, String name) {
		assertThat( scope.getDomainModel().collectTableMappings() )
				.noneMatch( table -> table.getName().equalsIgnoreCase( name ) );
	}

	static void assertNoColumn(Table table, String name) {
		assertThat( table.getColumn( new Column( name ) ) ).as( table.getName() + "." + name ).isNull();
	}
}
