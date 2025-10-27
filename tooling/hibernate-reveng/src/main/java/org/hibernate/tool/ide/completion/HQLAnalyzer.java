/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.ide.completion;

import org.hibernate.grammars.hql.HqlLexer;

import java.util.*;

public class HQLAnalyzer {

    /** Defines the HQL keywords. Based on hql.g antlr grammer in 2005 ;) */
    private static final String[] hqlKeywords = { "between", "class", "delete",
            "desc", "distinct", "elements", "escape", "exists", "false",
            "fetch", "from", "full", "group", "having", "in", "indices",
            "inner", "insert", "into", "is", "join", "left", "like", "new",
            "not", "null", "or", "order", "outer", "properties", "right",
            "select", "set", "some", "true", "union", "update", "versioned",
            "where", "and", "or", "as","on", "with",

            // -- SQL tokens --
            // These aren't part of HQL, but recognized by the lexer. Could be
            // usefull for having SQL in the editor..but for now we keep them out
            // "case", "end", "else", "then", "when",


            // -- EJBQL tokens --
            "both", "empty", "leading", "member", "object", "of", "trailing",
    };


    /**
     * built-in function names. Various normal builtin functions in SQL/HQL.
     * Maybe sShould try and do this dynamically based on dialect or
     * sqlfunctionregistry
     */
    private static final String[] builtInFunctions = {
            // standard sql92 functions
            "substring", "locate", "trim", "length", "bit_length", "coalesce",
            "nullif", "abs", "mod", "sqrt",
            "upper",
            "lower",
            "cast",
            "extract",

            // time functions mapped to ansi extract
            "second", "minute", "hour", "day",
            "month",
            "year",

            "str",

            // misc functions - based on oracle dialect
            "sign", "acos", "asin", "atan", "cos", "cosh", "exp", "ln", "sin",
            "sinh", "stddev", "sqrt", "tan", "tanh", "variance",

            "round", "trunc", "ceil", "floor",

            "chr", "initcap", "lower", "ltrim", "rtrim", "soundex", "upper",
            "ascii", "length", "to_char", "to_date",

            "current_date", "current_time", "current_timestamp", "lastday",
            "sysday", "systimestamp", "uid", "user",

            "rowid", "rownum",

            "concat", "instr", "instrb", "lpad", "replace", "rpad", "substr",
            "substrb", "translate",

            "substring", "locate", "bit_length", "coalesce",

            "atan2", "log", "mod", "nvl", "nvl2", "power",

            "add_months", "months_between", "next_day",

            "max", "min", };

    static {
        // to allow binary search
        Arrays.sort(builtInFunctions);
        Arrays.sort(hqlKeywords);
    }

    protected SimpleHQLLexer getLexer(char[] chars) {
        return new AntlrSimpleHQLLexer(chars);
    }

    /**
     * Returns true if the position is at a location where an entityname makes sense.
     * e.g. "from Pr| where x"
     */
    public boolean shouldShowEntityNames(String query, int cursorPosition) {
        return shouldShowEntityNames( query.toCharArray(), cursorPosition );
    }

    public boolean shouldShowEntityNames(char[] chars, int cursorPosition) {
        SimpleHQLLexer lexer = getLexer( chars);
        int tokenId = -1;
        boolean show = false;
        while ((tokenId = lexer.nextTokenId()) != HqlLexer.EOF) {
            if ((tokenId == HqlLexer.FROM ||
                    tokenId == HqlLexer.DELETE ||
                    tokenId == HqlLexer.UPDATE) &&
                    (lexer.getTokenOffset() + lexer.getTokenLength()) < cursorPosition) {
                show = true;
            } else if (tokenId != HqlLexer.DOT && tokenId != HqlLexer.AS && tokenId != HqlLexer.COMMA && tokenId != HqlLexer.IDENTIFIER && tokenId != HqlLexer.WS) {
                show = false;
            }
        }
        return show;
    }

    public List<SubQuery> getVisibleSubQueries(char[] chars, int position) {
        SubQueryList sqList = getSubQueries(chars, position);
        List<SubQuery> visible = new ArrayList<SubQuery>();
        for (SubQuery sq : sqList.subQueries) {
            if (sqList.caretDepth >= sq.depth && (sq.startOffset <= position || sq.endOffset >= position)) {
                visible.add(sq);
            }
        }
        return visible;
    }

    public List<EntityNameReference> getVisibleEntityNames(char[] chars, int position) {
        List<SubQuery> sqs = getVisibleSubQueries(chars, position);
        List<EntityNameReference> entityReferences = new ArrayList<EntityNameReference>();
        for (SubQuery sq : sqs) {
            entityReferences.addAll(sq.getEntityNames());
        }
        return entityReferences;
    }

    public SubQueryList getSubQueries(char[] query, int position) {
        SimpleHQLLexer syntax = getLexer( query );
        int numericId = -1;
        List<SubQuery> subQueries = new ArrayList<SubQuery>();
        int depth = 0;
        int caretDepth = 0;
        Map<Integer, SubQuery> level2SubQuery = new HashMap<Integer, SubQuery>();
        SubQuery current = null;
        while ((numericId = syntax.nextTokenId()) != HqlLexer.EOF) {
            boolean tokenAdded = false;
            if (numericId == HqlLexer.LEFT_PAREN) {
                depth++;
                if (position > syntax.getTokenOffset()) {
                    caretDepth = depth;
                }
            } else if (numericId == HqlLexer.RIGHT_PAREN) {
                SubQuery currentDepthQuery = level2SubQuery.get(depth);
                // We check if we have a query on the current depth.
                // If yes, we'll have to close it
                if (currentDepthQuery != null && currentDepthQuery.depth == depth) {
                    currentDepthQuery.endOffset = syntax.getTokenOffset();
                    currentDepthQuery.tokenIds.add(numericId);
                    currentDepthQuery.tokenText.add(String.valueOf(query, syntax.getTokenOffset(), syntax.getTokenLength()));
                    subQueries.add(currentDepthQuery);
                    level2SubQuery.remove(depth);
                    tokenAdded = true;
                }
                depth--;
                if (position > syntax.getTokenOffset()) {
                    caretDepth = depth;
                }
            }
            switch (numericId) {
                case HqlLexer.FROM:
                case HqlLexer.UPDATE:
                case HqlLexer.DELETE:
                case HqlLexer.SELECT:
                    if (!level2SubQuery.containsKey(depth)) {
                        current = new SubQuery();
                        current.depth = depth;
                        current.startOffset = syntax.getTokenOffset();
                        level2SubQuery.put(depth, current);
                    }
                    if (current != null) {
                        current.tokenIds.add(numericId);
                        current.tokenText.add(String.valueOf(query, syntax.getTokenOffset(), syntax.getTokenLength()));
                        break;
                    }
                default:
                    if (!tokenAdded) {
                        SubQuery sq = level2SubQuery.get(depth);
                        int i = depth;
                        while (sq == null && i >= 0) {
                            sq = level2SubQuery.get(i--);
                        }
                        if (sq != null) {
                            sq.tokenIds.add(numericId);
                            sq.tokenText.add(String.valueOf(query, syntax.getTokenOffset(), syntax.getTokenLength()));
                        }
                    }
            }
        }
        for (SubQuery sq : level2SubQuery.values()) {
            sq.endOffset = syntax.getTokenOffset() + syntax.getTokenLength();
            subQueries.add(sq);
        }
        Collections.sort(subQueries);
        SubQueryList sql = new SubQueryList();
        sql.caretDepth = caretDepth;
        sql.subQueries = subQueries;
        return sql;
    }


    /** Returns reference name found from position and backwards in the array.
     **/
    public static String getEntityNamePrefix(char[] chars, int position) {
        StringBuilder buff = new StringBuilder();
        for (int i = position - 1; i >= 0; i--) {
            char c = chars[i];
            if (c == '.' || Character.isJavaIdentifierPart(c)) {
                buff.insert(0, c);
            } else {
                break;
            }
        }
        return buff.toString();
    }

    public static class SubQueryList {

        int caretDepth;

        public List<SubQuery> subQueries;
    }


    static String[] getHQLKeywords() {
        return hqlKeywords;
    }

    static String[] getHQLFunctionNames() {
        return builtInFunctions;
    }

}
