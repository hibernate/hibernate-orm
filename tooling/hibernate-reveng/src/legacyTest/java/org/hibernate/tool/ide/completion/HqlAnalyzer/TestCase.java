/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hibernate.tool.ide.completion.HqlAnalyzer;

import org.hibernate.tool.ide.completion.EntityNameReference;
import org.hibernate.tool.ide.completion.HQLAnalyzer;
import org.hibernate.tool.ide.completion.SubQuery;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author leon
 */
public class TestCase {
	
	// TODO HBX-2064: Investigate and reenable
	@Disabled
    @Test
    public void testShouldShowTables() {
        String query = "select | from";
        doTestShouldShowTables(query, false);
        query = "select art from | Article1, Article2";
        doTestShouldShowTables(query, true);
        query = "from Article1, | Article2";
        doTestShouldShowTables(query, true);
        query = "select a, b, c | from Article a";
        doTestShouldShowTables(query, false);
        query = "select a, b, c from Article a where a in (select | from";
        doTestShouldShowTables(query, false);
        query = "select a, b, c from Article a where a in (select a from |";
        doTestShouldShowTables(query, true);
        query = "select a, b, c from Article a where a in (select a from C c where c.id in (select t from | G";
        doTestShouldShowTables(query, true);
        query = "select a from|";
        doTestShouldShowTables(query, false);
        query = "\n\nfrom Article art where art.|";
        doTestShouldShowTables(query, false);
        query = "update |";
        doTestShouldShowTables(query, true);
        query = "delete |";
        doTestShouldShowTables(query, true);
        query = "select new map(item.id as id, item.description as d, bid.amount as a) from |Item item join item.bids bid\r\n" + 
        		"    where bid.amount > 100";
        doTestShouldShowTables( query, true );
        
        query = "select new map(item.id| as id, item.description as d, bid.amount as a) from |Item item join item.bids bid\r\n" + 
		"    where bid.amount > 100";
        doTestShouldShowTables( query, false );
        
        query = "select new map(item.id as id, item.description as d, bid.amount as a) from Item item join item|.bids bid\r\n" + 
		"    where bid.amount > 100";
        doTestShouldShowTables( query, false );
        
        query = "from org.|hibernate";
        doTestShouldShowTables( query, true );
        
        query = """
                from\s
                \r\r
                org.|hibernate""";
        doTestShouldShowTables( query, true );
        
        query = """
                from\s
                \r\r
                org.hibernate\s
                \r where |""";
        doTestShouldShowTables( query, false );
        
        query = """
                from\s
                \r\r
                org.hibernate\s
                \r | where\s""";
        doTestShouldShowTables( query, true );

    }

    @Test
    public void testTableNamePrefix() {
        doTestPrefix("select a fromtable.substring(0, i0) Art|, Bart", "Art");
        doTestPrefix("from |", "");
        doTestPrefix("select a, b, c from Art,|", "");
        doTestPrefix("select u from | Garga", "");
        doTestPrefix("select t from A.B.C.D.|", "A.B.C.D.");
        doTestPrefix("from Goro|boro, Zoroor", "Goro");
    }

    @Test
    public void testSubQueries() {
        doTestSubQueries("select a", 1);
        doTestSubQueries("fr", 0);
        doTestSubQueries("from Article a", 1);
        doTestSubQueries("select a from A, B, C", 1);
        doTestSubQueries("select a from T a where a.id in (   select c from C c)", 2);
        doTestSubQueries("select c where c.id in (select D from D D)", 2);
        doTestSubQueries("select d from D d where d.id in (select a.id from A a where a.id in (select b.id from B b", 3);
    }

    @Test
    public void testVisibleSubQueries() {
        doTestVisibleSubQueries("select | from A a join a.b b", 1);
        doTestVisibleSubQueries("select | from A a join a.b b where b.id in (select c.id from C c)", 1);
        doTestVisibleSubQueries("select a from A a join a.b b where b.id in (select c.id from | C c)", 2);
        doTestVisibleSubQueries("select a from A a join a.b b where b.id in (select c.id from C c) and b.| > 2", 1);
        doTestVisibleSubQueries("select a from A a where | a.id in (select b.id from B b where b.id in (select c.id", 1);
        doTestVisibleSubQueries("select a from A a where a.id in (select | b.id from B b where b.id in (select c.id", 2);
        doTestVisibleSubQueries("select a from A a where a.id in (select b.id from B b where b.id in (select c.id |", 3);
    }

    public void doTestVisibleSubQueries(String query, int size) {
    	char[] cs = query.replaceAll("\\|", "").toCharArray();
    	List<SubQuery> visible = new HQLAnalyzer().getVisibleSubQueries(cs, query.indexOf("|"));
        assertEquals(size, visible.size(), "Invalid visible query size");
    }

    private void doTestSubQueries(String query, int size) {    	
    	List<SubQuery> l = new HQLAnalyzer().getSubQueries(query.toCharArray(), 0).subQueries;
        assertEquals(size, l.size(), "Incorrect subqueries count");
    }

    private void doTestPrefix(String query, String prefix) {
        assertEquals(prefix, HQLAnalyzer.getEntityNamePrefix(query.toCharArray(), query.indexOf("|")));
    }

    private void doTestShouldShowTables(String query, boolean expectedValue) {
        char[] ch = query.replaceAll("\\|", "").toCharArray();
		if (expectedValue) {
            assertTrue(new HQLAnalyzer().shouldShowEntityNames(ch, getCaretPosition(query)));
        } else {
            assertFalse(new HQLAnalyzer().shouldShowEntityNames(ch, getCaretPosition(query)));
        }
    }

    @Test
    public void testVisibleTablesInUpdates() {
        doTestVisibleTables("update Article set |id = 10",
                new String[] { "Article" },
                new String[] { "Article" });
        doTestVisibleTables("update Article art set id = 100 where id in (select price.article.id from Price price |",
                new String[] { "Article", "Price" },
                new String[] { "art", "price" });
        doTestVisibleTables("update Article set id = 100 where id in (select price.article.id from Price price |",
                new String[] { "Article", "Price" },
                new String[] { "Article", "price" });
        doTestVisibleTables("update Article set id = 100 | where id in (select price.article.id from Price price",
                new String[] { "Article" },
                new String[] { "Article" });
    }

    @Test
    public void testVisibleTablesInDeletes() {
        doTestVisibleTables("delete Article where id = 10",
                new String[] { "Article" },
                new String[] { "Article" });
        doTestVisibleTables("delete Article art where id in (select price.article.id from Price price |",
                new String[] { "Article", "Price" },
                new String[] { "art", "price" });
        doTestVisibleTables("delete Article where id in (select price.article.id from Price price |",
                new String[] { "Article", "Price" },
                new String[] { "Article", "price" });
        doTestVisibleTables("delete Article | where id in (select price.article.id from Price price",
                new String[] { "Article" },
                new String[] { "Article" });
    }

    @Test
    public void testVisibleTablesInQueries() {
        doTestVisibleTables("from Article |",
                new String[] { "Article" },
                new String[] { "Article" });
        doTestVisibleTables("from | Article art",
                new String[] { "Article" },
                new String[] { "art" });
        doTestVisibleTables("select | art.id from Article art as art",
                new String[] { "Article" },
                new String[] { "art" });
        doTestVisibleTables("select | art.id from Article as art, Company c",
                new String[] { "Article", "Company" },
                new String[] { "art", "c" });
        doTestVisibleTables("from com.Article a, com.Company c, T.T.T f |",
                new String[] { "com.Article", "com.Company", "T.T.T" },
                new String[] { "a", "c", "f" });
        doTestVisibleTables("| from Article a left join a.B as b join b.ddd GuGu right outer join GuGu FF inner join FF.T",
                new String[] { "Article", "a.B", "b.ddd", "GuGu", "FF.T" },
                new String[] { "a", "b", "GuGu", "FF", "FF.T" });
        doTestVisibleTables("from Article art where art.id in (select c.id from C c) | and art.id > 100",
                new String[] { "Article" },
                new String[] { "art" });
        doTestVisibleTables("from Article art where art.id in (select c.id | from C c) and art.id > 100",
                new String[] { "Article", "C" },
                new String[] { "art", "c" });
        doTestVisibleTables("from A a where a.id in ((((select b.id from B b where " +
                "b.id = ((((select c.id from C c where c.id in ((((select d.id from D d where |",
                new String[] { "A", "B", "C", "D" },
                new String[] { "a", "b", "c", "d" });
        doTestVisibleTables("from A a where a.id in ((((select b.id from B b where " +
                "b.id = ((((select c.id from C c where c.id in ((((select d.id from D d where)))) |",
                new String[] { "A", "B", "C" },
                new String[] { "a", "b", "c" });
        doTestVisibleTables("from A a where a.id in ((((select b.id from B b where " +
                "b.id = ((((select c.id from C c where c.id in ((((select d.id from D d where)))) |",
                new String[] { "A", "B", "C" },
                new String[] { "a", "b", "c" });
        doTestVisibleTables("from A a where a.id in ((((select b.id from B b where " +
                "b.id = ((((select c.id from C c where c.id in ((((select d.id from D d where))))) |",
                new String[] { "A", "B" },
                new String[] { "a", "b" });
        doTestVisibleTables("from A a where a.id in ((((select b.id from B b where " +
                "b.id = ((((select c.id from C c where c.id in ((((select d.id from D d where)))))|)",
                new String[] { "A", "B" },
                new String[] { "a", "b" });
        doTestVisibleTables("from A a where a.id in ((((select b.id from B b where " +
                "b.id = ((((select c.id from C c where c.id in ((((select d.id from D d where)))))))))  |)",
                new String[] { "A" },
                new String[] { "a" });
        doTestVisibleTables("(|from A a)",
                new String[] { "A" },
                new String[] { "a" });
        doTestVisibleTables("|(from A a)",
                new String[] { },
                new String[] { });
        doTestVisibleTables("(from A a|)",
                new String[] { "A" },
                new String[] { "a" });
        doTestVisibleTables("(from A a)|",
                new String[] { },
                new String[] { });
        doTestVisibleTables("select upper(a.id |) from A a",
                new String[] { "A" },
                new String[] { "a" });
        doTestVisibleTables("select new Stuff(a.id, |b.id) from A a left outer join a.B b",
                new String[] { "A", "a.B" },
                new String[] { "a", "b" });
        doTestVisibleTables("select new Stuff(a.id, b.id) from A a left outer join a.B b where b.id in (select c.id from C c join c.stuff stuffer where c|",
                new String[] { "A", "a.B", "C", "c.stuff" },
                new String[] { "a", "b", "c", "stuffer" });
        doTestVisibleTables("from A a, B, C c where c.id not in (select | d from D",
                new String[] { "A", "B", "C", "D"},
                new String[] { "a", "B", "c", "D"});
    }

    private void doTestVisibleTables(String query, String[] types, String[] aliases) {
        char[] toCharArray = query.replaceAll("\\|", "").toCharArray();
		List<EntityNameReference> qts = new HQLAnalyzer().getVisibleEntityNames(toCharArray, getCaretPosition(query));
        assertEquals(types.length, qts.size(), "Incorrect table count");
        int i = 0;
        for (EntityNameReference qt : qts) {
            assertEquals(types[i], qt.getEntityName(), "Incorrect query table type [" + i + "]");
            assertEquals(aliases[i++], qt.getAlias(), "Incorrect query table alias [" + i + "]");
        }
    }

    private int getCaretPosition(String str) {
        int indexOf = str.indexOf("|");
		return indexOf!=-1?indexOf:str.length();
    }
    
}
