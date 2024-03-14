package org.hibernate.orm.test.customsql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.jdbc.Expectation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = CustomSqlPrimaryTableExplicitTest.Custom.class)
public class CustomSqlPrimaryTableExplicitTest {
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
    @SQLInsert(table = "CustomPrimary",
            sql="insert into CustomPrimary (name, revision, id) values (upper(?),?,?)",
            verify = Expectation.RowCount.class)
    @SQLInsert(table = "CustomSecondary",
            sql="insert into CustomSecondary (text, id) values (upper(?),?)",
            verify = Expectation.None.class)
    @SQLUpdate(table = "CustomPrimary",
            sql="update CustomPrimary set name = upper(?), revision = ? where id = ? and revision = ?",
            verify = Expectation.RowCount.class)
    @SQLUpdate(table = "CustomSecondary",
            sql="update CustomSecondary set text = upper(?) where id = ?",
            verify = Expectation.None.class)
    @SQLDelete(table = "CustomPrimary",
            sql="update CustomPrimary set name = 'DELETED' where id = ? and revision = ?",
            verify = Expectation.RowCount.class)
    @SQLDelete(table = "CustomSecondary",
            sql="update CustomSecondary set text = 'DELETED' where id = ?",
            verify = Expectation.None.class)
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

