/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.LocalDateJavaType;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKey;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = ReservedKeywordInFilterTest.SimpleValue.class
)
@SessionFactory
@JiraKey( "HHH-18570" )
public class ReservedKeywordInFilterTest {

	@Test
	public void testReservedKeywordInFilterTest(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					SimpleValue value = new SimpleValue( 1, LocalDate.now() );
					session.persist( value );
				}
		);
		
		scope.inTransaction(
				session -> {
					session
					.enableFilter( "dateFilter" )
					.setParameter( "startDate", LocalDate.now().minusDays( 5 ) )
					.setParameter( "endDate", LocalDate.now().plusDays( 5 ) );
					
					// Check that the generated SQL executes by loading some entities and associations
					List<SimpleValue> values = session.createQuery( "select v from SimpleValue v", SimpleValue.class ).list();

					assertTrue( values.size() == 1 );
					for (SimpleValue value : values) {
						assertTrue( value.getChildren().isEmpty() );
					}
				}
		);
	}

	@Entity(name = "SimpleValue")
	@FilterDef(
			name = "dateFilter",
			parameters = { 
					@ParamDef(name = "startDate", type = LocalDateJavaType.class), 
					@ParamDef(name = "endDate", type = LocalDateJavaType.class) })
	@Filter(name = "dateFilter", condition = "(date between :startDate and :endDate)")
	public static class SimpleValue {
		private Integer id;

		private LocalDate date;
		
		private SimpleValue parent;
		
		private Map<LocalDate, SimpleValue> children;

		public SimpleValue() {
		}

		public SimpleValue(Integer id, LocalDate date) {
			this.id = id;
			this.date = date;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}

		@ManyToOne
		@JoinColumn(name = "parent_id")
		public SimpleValue getParent() {
			return parent;
		}

		public void setParent(SimpleValue parent) {
			this.parent = parent;
		}

		@Filter(name = "dateFilter", condition = "(date between :startDate and :endDate)")
		@OneToMany(mappedBy = "parent")
		@MapKey(name = "date")
		public Map<LocalDate, SimpleValue> getChildren() {
			return children;
		}

		public void setChildren(Map<LocalDate, SimpleValue> children) {
			this.children = children;
		}
	}
}
