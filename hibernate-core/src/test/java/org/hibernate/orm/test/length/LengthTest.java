package org.hibernate.orm.test.length;

import org.hibernate.Length;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {WithLongStrings.class,WithLongTypeStrings.class})
@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix rowsize to exceed the allowable limit (32767).")
public class LengthTest {
    @Test
    public void testLength(SessionFactoryScope scope) {
        WithLongStrings strings = new WithLongStrings();
        strings.longish = "hello world ".repeat(2500);
        strings.long16 = "hello world ".repeat(2700);
        strings.long32 = "hello world ".repeat(20000);
        strings.clob = "hello world ".repeat(40000);
        scope.inTransaction(s -> s.persist(strings));
        scope.inTransaction(s -> {
            WithLongStrings strs = s.find(WithLongStrings.class, strings.id);
            assertEquals(strs.longish, strings.longish);
            assertEquals(strs.long16, strings.long16);
            assertEquals(strs.long32, strings.long32);
            assertEquals(strs.clob, strings.clob);
        });
    }

    @Test
    public void testSqlType(SessionFactoryScope scope) {
        WithLongTypeStrings strings = new WithLongTypeStrings();
        strings.longish = "hello world ".repeat(2500);
        strings.long32 = "hello world ".repeat(20000);
        scope.inTransaction(s -> s.persist(strings));
        scope.inTransaction(s -> {
            WithLongTypeStrings strs = s.find(WithLongTypeStrings.class, strings.id);
            assertEquals(strs.longish, strings.longish);
            assertEquals(strs.long32, strings.long32);
        });
    }

    @Test
    public void testLong32(SessionFactoryScope scope) {
        final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
        final BasicValuedMapping mapping = (BasicValuedMapping) scope.getSessionFactory()
                .getRuntimeMetamodels()
                .getMappingMetamodel()
                .getEntityDescriptor( WithLongStrings.class )
                .findAttributeMapping( "long32" );
        if ( dialect.useMaterializedLobWhenCapacityExceeded() && Length.LONG32 > dialect.getMaxVarcharCapacity() ) {
            assertEquals( SqlTypes.CLOB, mapping.getJdbcMapping().getJdbcType().getJdbcTypeCode() );
        }
        else {
            assertEquals( SqlTypes.VARCHAR, mapping.getJdbcMapping().getJdbcType().getJdbcTypeCode() );
        }
    }
}
