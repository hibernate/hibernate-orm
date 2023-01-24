package org.hibernate.orm.test.secondarytable;

import java.time.LocalDateTime;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {Record.class,SpecialRecord.class})
@SessionFactory
public class SecondaryRowTest {
	@Test
	@SkipForDialect(
			dialectClass = H2Dialect.class,
			reason = "This test relies on SQL execution counts which is based on the legacy multi-statement solution.  " +
					"HHH-16084 adds support for H2 MERGE which handles those cases in one statement, so the counts are off.  " +
					"Need to change this test to physically check the number of rows.  " +
					"See e.g. org.hibernate.orm.test.write.UpsertTests"
	)
	public void testSecondaryRow(SessionFactoryScope scope) {
		// we need to check the actual number of rows.
		// because we now support merge/upsert SQL statements, the
		// because HHH-16084 implements support for  usage
		int seq = scope.getSessionFactory().getJdbcServices().getDialect().getSequenceSupport().supportsSequences()
				? 1 : 0;

		Record record = new Record();
		record.enabled = true;
		record.text = "Hello World!";
		scope.inTransaction(s -> s.persist(record));
		scope.getCollectingStatementInspector().assertExecutedCount(2+seq);
		scope.getCollectingStatementInspector().clear();

		Record record2 = new Record();
		record2.enabled = true;
		record2.text = "Hello World!";
		record2.comment = "Goodbye";
		scope.inTransaction(s -> s.persist(record2));
		scope.getCollectingStatementInspector().assertExecutedCount(3+seq);
		scope.getCollectingStatementInspector().clear();

		SpecialRecord specialRecord = new SpecialRecord();
		specialRecord.enabled = true;
		specialRecord.text = "Hello World!";
		specialRecord.validated = LocalDateTime.now();
		scope.inTransaction(s -> s.persist(specialRecord));
		scope.getCollectingStatementInspector().assertExecutedCount(3+seq);
		scope.getCollectingStatementInspector().clear();

		scope.inTransaction(s -> assertNotNull(s.find(Record.class, record.id)));
		scope.getCollectingStatementInspector().assertExecutedCount(1);
		scope.getCollectingStatementInspector().clear();

		scope.inTransaction(s -> assertNotNull(s.find(Record.class, record2.id)));
		scope.getCollectingStatementInspector().assertExecutedCount(1);
		scope.getCollectingStatementInspector().clear();

		scope.inTransaction(s -> assertNotNull(s.find(Record.class, specialRecord.id)));
		scope.getCollectingStatementInspector().assertExecutedCount(1);
		scope.getCollectingStatementInspector().clear();

		scope.inTransaction(s -> assertEquals(3, s.createQuery("from Record").getResultList().size()));
		scope.getCollectingStatementInspector().assertExecutedCount(1);
		scope.getCollectingStatementInspector().clear();

		scope.inTransaction(s -> assertEquals(1, s.createQuery("from SpecialRecord").getResultList().size()));
		scope.getCollectingStatementInspector().assertExecutedCount(1);
		scope.getCollectingStatementInspector().clear();

		scope.inTransaction(s -> {
			Record r = s.find(Record.class, record.id);
			r.text = "new text";
			r.comment = "the comment";
		});
		scope.getCollectingStatementInspector().assertExecutedCount(3);
		assertTrue( scope.getCollectingStatementInspector().getSqlQueries().get(1).startsWith("update ") );
		assertTrue( scope.getCollectingStatementInspector().getSqlQueries().get(2).startsWith("insert ") );
		scope.getCollectingStatementInspector().clear();

		scope.inTransaction(s -> {
			Record r = s.find(Record.class, record.id);
			r.comment = "new comment";
		});
		scope.getCollectingStatementInspector().assertExecutedCount(2);
		assertTrue( scope.getCollectingStatementInspector().getSqlQueries().get(1).startsWith("update ") );
		scope.getCollectingStatementInspector().clear();

		scope.inTransaction(s -> {
			Record r = s.find(Record.class, record2.id);
			r.comment = null;
		});
		scope.getCollectingStatementInspector().assertExecutedCount(2);
		assertTrue( scope.getCollectingStatementInspector().getSqlQueries().get(1).startsWith("delete ") );
		scope.getCollectingStatementInspector().clear();

		scope.inTransaction(s -> {
			SpecialRecord r = s.find(SpecialRecord.class, specialRecord.id);
			r.validated = null;
			r.timestamp = System.currentTimeMillis();
		});
		scope.getCollectingStatementInspector().assertExecutedCount(2);
		assertTrue( scope.getCollectingStatementInspector().getSqlQueries().get(1).startsWith("delete ") );
		scope.getCollectingStatementInspector().clear();
	}
}
