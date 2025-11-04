/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.orm.test.annotations.query.Attrset;
import org.hibernate.orm.test.annotations.query.Attrvalue;
import org.hibernate.orm.test.annotations.query.Employee;
import org.hibernate.orm.test.annotations.query.Employeegroup;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		Attrset.class,
		Attrvalue.class,
		Employee.class,
		Employeegroup.class,
		Panel.class,
		TrtPanel.class
})
@SessionFactory
public class DeleteWithSubqueryTest {

	@Test
	@JiraKey( value = "HHH-8318" )
	@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true,
			reason = "Cannot use Attrvalue in the delete and from clauses simultaneously." )
	public void testDeleteMemberOf(SessionFactoryScope factoryScope) {
		final String qry = """
				delete Attrvalue aval
				where aval.id in (
					select val2.id
					from Employee e,
						Employeegroup eg,
						Attrset aset,
						Attrvalue val2
					where eg.id = e.employeegroup.id
						and aset.id = e.attrset.id
						and val2 member of aset.attrvalues
				)
				""";
		factoryScope.inTransaction( session -> {
			session.createQuery( qry ).executeUpdate();
		} );
	}

	@Test
	@JiraKey( value = "HHH-8447" )
	public void testDeleteMultipleWhereIns(SessionFactoryScope factoryScope) {
		var hql = """
				DELETE FROM Panel panelEntity
				WHERE panelEntity.clientId IN (
					SELECT trtPanel.clientId
					FROM TrtPanel trtPanel
				)
				AND panelEntity.deltaStamp NOT IN (
					SELECT trtPanel.deltaStamp
					FROM TrtPanel trtPanel
				)
				""";
		factoryScope.inTransaction( session -> {
			session.createQuery( hql ).executeUpdate();
		} );
	}
}
