package org.hibernate.tool.ide.completion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;

public class SubQuery implements Comparable<SubQuery> {

    public int compareTo(SubQuery s) {
        return startOffset - s.startOffset;
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
        int lastToken = HqlSqlTokenTypes.EOF;
        for (Iterator<Integer> iter = tokenIds.iterator(); iter.hasNext();) {
			Integer typeInteger = (Integer) iter.next();
			int type = typeInteger.intValue();
			if (!cont) {
                break;
            }
            if (!afterFrom &&
                    (type == HqlSqlTokenTypes.FROM ||
                    type == HqlSqlTokenTypes.UPDATE ||
                    type == HqlSqlTokenTypes.DELETE)) {
                afterFrom = true;
            } else if (afterJoin) {
                switch (type) {
                    case HqlSqlTokenTypes.ORDER:
                    case HqlSqlTokenTypes.WHERE:
                    case HqlSqlTokenTypes.GROUP:
                    case HqlSqlTokenTypes.HAVING:
                        cont = false;
                        break;
                    case HqlSqlTokenTypes.INNER:
                    case HqlSqlTokenTypes.OUTER:
                    case HqlSqlTokenTypes.LEFT:
                    case HqlSqlTokenTypes.RIGHT:
                    case HqlSqlTokenTypes.JOIN:
                        joins.append(",");
                        break;
                    case HqlSqlTokenTypes.COMMA: 
                    	joins.append(","); //TODO: we should detect this and create the list directly instead of relying on the tokenizer
                    	break;
                    case HqlSqlTokenTypes.DOT:
                    	joins.append("."); 
                    	break;
                    case HqlSqlTokenTypes.IDENT:
                    	if(lastToken!=HqlSqlTokenTypes.DOT) {
                    		joins.append(" ");
                    	} 
                        joins.append(tokenText.get(i));
                        break;
                }
            } else if (afterFrom) {
                switch (type) {
                    case HqlSqlTokenTypes.ORDER:
                    case HqlSqlTokenTypes.WHERE:
                    case HqlSqlTokenTypes.GROUP:
                    case HqlSqlTokenTypes.HAVING:
                    case HqlSqlTokenTypes.SET:
                        cont = false;
                        break;
                    case HqlSqlTokenTypes.COMMA: 
                    	tableNames.append(","); //TODO: we should detect this and create the list directly instead of relying on the tokenizer
                    	break;
                    case HqlSqlTokenTypes.DOT:
                    	tableNames.append("."); 
                    	break;
                    case HqlSqlTokenTypes.IDENT:
                    	if(lastToken!=HqlSqlTokenTypes.DOT) {
                    		tableNames.append(" ");
                    	} 
                        tableNames.append(tokenText.get(i));
                        break;
                    case HqlSqlTokenTypes.JOIN:
                    	tableNames.append(",");
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
            if (table.indexOf(' ') == -1 && table.length() > 0) {
                tables.add(new EntityNameReference(table, table));
            } else {
                StringTokenizer aliasTokenizer = new StringTokenizer(table, " ");
                if (aliasTokenizer.countTokens() >= 2) {
                    String type = aliasTokenizer.nextToken().trim();
                    String alias = aliasTokenizer.nextToken().trim();
                    if (type.length() > 0 && alias.length() > 0) {
                        tables.add(new EntityNameReference(type, alias));
                    }
                }
            }
        }
    }
}
