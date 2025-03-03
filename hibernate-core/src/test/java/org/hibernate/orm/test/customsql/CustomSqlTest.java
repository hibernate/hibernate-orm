/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.customsql;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = CustomSqlTest.Custom.class)
public class CustomSqlTest {
	@Test
	public void testCustomSql(SessionFactoryScope scope) {
		Custom c = new Custom();
		c.name = "name";
		c.text = "text";
		scope.inTransaction(s->{
			s.persist(c);
			s.flush();
			s.clear();
			Custom cc = s.find(Custom.class, c.id);
			assertEquals(cc.text, "TEXT");
			assertEquals(cc.name, "NAME");
			cc.name = "eman";
			cc.text = "more text";
			s.flush();
			s.clear();
			cc = s.find(Custom.class, c.id);
			assertEquals(cc.text, "MORE TEXT");
			assertEquals(cc.name, "EMAN");
			s.remove(cc);
			s.flush();
			s.clear();
			cc = s.find(Custom.class, c.id);
			assertEquals(cc.text, "DELETED");
			assertEquals(cc.name, "DELETED");
		});
	}
	@Entity
	@Table(name = "CustomPrimary")
	@SecondaryTable(name = "CustomSecondary")
	@SQLInsert(sql="insert into CustomPrimary (name, revision, id) values (upper(?),?,?)")
	@SQLInsert(table = "CustomSecondary", sql="insert into CustomSecondary (text, id) values (upper(?),?)")
	@SQLUpdate(sql="update CustomPrimary set name = upper(?), revision = ? where id = ? and revision = ?")
	@SQLUpdate(table = "CustomSecondary", sql="update CustomSecondary set text = upper(?) where id = ?")
	@SQLDelete(sql="update CustomPrimary set name = 'DELETED' where id = ? and revision = ?")
	@SQLDelete(table = "CustomSecondary", sql="update CustomSecondary set text = 'DELETED' where id = ?")
	static class Custom {
		@Id @GeneratedValue
		Long id;
		@Version @Column(name = "revision")
		int version;
		String name;
		@Column(table = "CustomSecondary")
		String text;
	}
}
