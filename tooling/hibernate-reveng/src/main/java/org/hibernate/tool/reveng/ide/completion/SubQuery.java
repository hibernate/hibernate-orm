/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2015-2025 Red Hat, Inc.
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
package org.hibernate.tool.reveng.ide.completion;

import org.hibernate.grammars.hql.HqlLexer;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class SubQuery implements Comparable<SubQuery> {

	public int compareTo(SubQuery s) {
		return startOffset - s.startOffset;
	}

	public boolean equals(Object s) {
		if (!(s instanceof SubQuery)) return false;
		return startOffset == ((SubQuery)s).startOffset;
	}

	public int hashCode() {
		return startOffset;
	}

	List<Integer> tokenIds = new ArrayList<Integer>();

    List<String> tokenText = new ArrayList<String>();

    int startOffset;

    int endOffset;
    
    int depth;

    public int getTokenCount() {
        return tokenIds.size();
    }

    public int getToken(int i) {
        return tokenIds.get(i);
    }

    public String getTokenText(int i) {
        return (String) tokenText.get(i);
    }

    public List<EntityNameReference> getEntityNames() {
        boolean afterFrom = false;
        boolean afterJoin = false;
        StringBuffer tableNames = new StringBuffer();
        StringBuffer joins = new StringBuffer();
        int i = 0;
        boolean cont = true;
        int lastToken = HqlLexer.EOF;
		for ( Integer typeInteger : tokenIds ) {
			int type = typeInteger;
			if ( !cont ) {
				break;
			}
			if ( !afterFrom &&
				 (type == HqlLexer.FROM ||
				  type == HqlLexer.UPDATE ||
				  type == HqlLexer.DELETE) ) {
				afterFrom = true;
			}
			else if ( afterJoin ) {
				switch ( type ) {
					case HqlLexer.ORDER:
					case HqlLexer.WHERE:
					case HqlLexer.GROUP:
					case HqlLexer.HAVING:
						cont = false;
						break;
					case HqlLexer.INNER:
					case HqlLexer.OUTER:
					case HqlLexer.LEFT:
					case HqlLexer.RIGHT:
					case HqlLexer.JOIN:
						joins.append( "," );
						break;
					case HqlLexer.COMMA:
						joins.append(
								"," ); //TODO: we should detect this and create the list directly instead of relying on the tokenizer
						break;
					case HqlLexer.DOT:
						joins.append( "." );
						break;
					case HqlLexer.IDENTIFIER:
						if ( lastToken != HqlLexer.DOT ) {
							joins.append( " " );
						}
						joins.append( tokenText.get( i ) );
						break;
				}
			}
			else if ( afterFrom ) {
				switch ( type ) {
					case HqlLexer.ORDER:
					case HqlLexer.WHERE:
					case HqlLexer.GROUP:
					case HqlLexer.HAVING:
					case HqlLexer.SET:
						cont = false;
						break;
					case HqlLexer.COMMA:
						tableNames.append(
								"," ); //TODO: we should detect this and create the list directly instead of relying on the tokenizer
						break;
					case HqlLexer.DOT:
						tableNames.append( "." );
						break;
					case HqlLexer.IDENTIFIER:
						if ( lastToken != HqlLexer.DOT ) {
							tableNames.append( " " );
						}
						tableNames.append( tokenText.get( i ) );
						break;
					case HqlLexer.JOIN:
						tableNames.append( "," );
						afterJoin = true;
						break;
					default:
						break;
				}
			}
			i++;
			lastToken = type;
		}
        List<EntityNameReference> tables = new ArrayList<EntityNameReference>();
        addEntityReferences(tables, tableNames);
        addEntityReferences(tables, joins);
        return tables;
    }

    private void addEntityReferences(final List<EntityNameReference> tables, final StringBuffer tableNames) {
        StringTokenizer tableTokenizer = new StringTokenizer(tableNames.toString(), ",");
        while (tableTokenizer.hasMoreTokens()) {
            String table = tableTokenizer.nextToken().trim();
            if (table.indexOf(' ') == -1 && !table.isEmpty() ) {
                tables.add(new EntityNameReference(table, table));
            } else {
                StringTokenizer aliasTokenizer = new StringTokenizer(table, " ");
                if (aliasTokenizer.countTokens() >= 2) {
                    String type = aliasTokenizer.nextToken().trim();
                    String alias = aliasTokenizer.nextToken().trim();
                    if ( !type.isEmpty() && !alias.isEmpty() ) {
                        tables.add(new EntityNameReference(type, alias));
                    }
                }
            }
        }
    }
}
