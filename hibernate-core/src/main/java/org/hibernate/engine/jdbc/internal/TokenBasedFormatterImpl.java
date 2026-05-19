/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import org.hibernate.grammars.sql.SqlFormatterLexer;
import org.jboss.logging.Logger;

/**
 * Advanced SQL formatter using ANTLR-based tokenization.
 * Provides better formatting for complex SQL queries compared to BasicFormatterImpl.
 *
 * @author Hibernate Team
 */
public class TokenBasedFormatterImpl implements Formatter {

	private static final Logger log = Logger.getLogger( TokenBasedFormatterImpl.class );

	private static final String INDENT = "\t";
	private static final String LINE_SEPARATOR = System.lineSeparator();

	@Override
	public String format(String source) {
		if (source == null || source.isBlank()) {
			return source;
		}

		try {
			final SqlFormatterLexer lexer = new SqlFormatterLexer(CharStreams.fromString(source));
			return new FormatProcess(lexer.getAllTokens()).format();
		}
		catch (Exception e) {
			// Log the error and fall back to unformatted SQL if formatting fails
			log.debugf(e, "Error formatting SQL statement: %s", source);
			return source;
		}
	}

	private static class FormatProcess {
		private final List<Token> tokens;
		private final StringBuilder output = new StringBuilder();
		private int position = 0;
		private int baseIndent = 0; // Base indent level for current context
		private boolean atLineStart = true;
		private final Stack<Context> contextStack = new Stack<>();
		private boolean inMergeWhenThenBlock = false; // Tracks if we're in a MERGE WHEN THEN block

		// Context types
		private enum ContextType {
			MAIN,           // Main query
			CTE,            // Common Table Expression
			SUBQUERY,       // Subquery (in FROM/JOIN/WHERE/assignment/etc.)
			FUNCTION,       // Function call
			CASE_EXPR,      // CASE expression
			OPERATOR_LIST   // Operator list (IN, ALL, etc.)
		}

		private static class Context {
			final ContextType type;
			final int indent;
			final  boolean isMultiline; // For operator lists - tracks if list was made multi-line
			boolean firstClauseElement; // For first condition in WHERE/ON, first item in SELECT list, etc.

			Context(ContextType type, int indent) {
				this( type, indent, false, false );
			}

			Context(ContextType type, int indent, boolean isMultiline, boolean isFirstClauseElement) {
				this.type = type;
				this.indent = indent;
				this.isMultiline = isMultiline;
				this.firstClauseElement = isFirstClauseElement;
			}
		}

		FormatProcess(List<? extends Token> tokens) {
			this.tokens = new ArrayList<>(tokens);
			// Filter out whitespace tokens
			this.tokens.removeIf(t -> t.getType() == SqlFormatterLexer.WS);
			// Start with main context
			contextStack.push(new Context(ContextType.MAIN, 0));
		}

		String format() {
			while (position < tokens.size()) {
				processToken(currentToken());
				position++;
			}
			return output.toString().trim();
		}

		private void processToken(Token token) {
			final int type = token.getType();

			// Handle specific token types
			switch (type) {
				case SqlFormatterLexer.LINE_COMMENT:
					processLineComment(token);
					break;
				case SqlFormatterLexer.BLOCK_COMMENT:
					processBlockComment( token );
					break;
				case SqlFormatterLexer.WITH,
					SqlFormatterLexer.ORDER,
					SqlFormatterLexer.GROUP,
					SqlFormatterLexer.OFFSET,
					SqlFormatterLexer.FETCH,
					SqlFormatterLexer.LIMIT,
					SqlFormatterLexer.UNION,
					SqlFormatterLexer.INTERSECT,
					SqlFormatterLexer.EXCEPT,
					SqlFormatterLexer.INSERT,
					SqlFormatterLexer.DELETE,
					SqlFormatterLexer.MERGE:
					newLineIfNeeded();
					baseIndent = currentContext().indent;
					writeToken(token);
					break;
				case SqlFormatterLexer.UPDATE:
					// UPDATE can be:
					// 1. UPDATE table ... (DML statement - newline)
					// 2. DO UPDATE (after ON CONFLICT - same line)
					// 3. UPDATE inside MERGE WHEN MATCHED (indented)
					Token updatePrev = peekBack(1);
					if (updatePrev != null && updatePrev.getType() == SqlFormatterLexer.DO) {
						// DO UPDATE - stay on same line
						space();
						writeToken(token);
					}
					else {
						// Regular UPDATE statement or MERGE UPDATE
						newLineIfNeeded();
						baseIndent = currentContext().indent;
						writeToken(token);
					}
					break;
				case SqlFormatterLexer.INTO:
					space();
					writeToken(token);
					break;
				case SqlFormatterLexer.USING:
					// USING in MERGE statement - newline at MERGE context level
					newLineIfNeeded();
					baseIndent = currentContext().indent;
					writeToken(token);
					break;
				case SqlFormatterLexer.VALUES:
					// VALUES can be:
					// 1. Major VALUES clause in INSERT (newline)
					// 2. VALUES(col) function in MySQL ON DUPLICATE KEY UPDATE (inline)
					Token valuesPrev = peekBack(1);
					if (valuesPrev != null && valuesPrev.getType() == SqlFormatterLexer.EQ) {
						// VALUES(col) function in UPDATE assignment
						space();
						writeToken(token);
					}
					else {
						// Major VALUES clause
						newLineIfNeeded();
						baseIndent = currentContext().indent;
						writeToken(token);
					}
					break;
				case SqlFormatterLexer.RETURNING:
					newLineIfNeeded();
					baseIndent = currentContext().indent;
					writeToken(token);
					break;
				case SqlFormatterLexer.SET:
					processSetClause(token);
					break;
				case SqlFormatterLexer.ON:
					processOnKeyword(token);
					break;
				case SqlFormatterLexer.DO:
					processDo(token);
					break;
				case SqlFormatterLexer.WHEN:
					processWhen(token);
					break;
				case SqlFormatterLexer.AS:
					space();
					writeToken(token);
					// Check if followed by (
					Token next = peekAhead(1);
					if (next != null && next.getType() == SqlFormatterLexer.LPAREN) {
						space();
					}
					break;
				case SqlFormatterLexer.SELECT:
					newLineIfNeeded();
					baseIndent = currentContext().indent;
					writeToken(token);
					// Check if multi-line SELECT list
					if (shouldListBeMultiline(SqlFormatterLexer.FROM, SqlFormatterLexer.RPAREN)) {
						newLine();
						baseIndent = currentContext().indent + 1;
						currentContext().firstClauseElement = true;
					}
					break;
				case SqlFormatterLexer.FROM:
					// Check if we're following a DELETE statement
					if (isAfterKeyword(SqlFormatterLexer.DELETE, 1)) {
						space();
						writeToken(token);
					}
					// Check if we're inside a function (like TRIM)
					else if (currentContext().type == ContextType.FUNCTION) {
						// Inside function - just add space and write
						space();
						writeToken(token);
					}
					else {
						// Major FROM clause
						newLineIfNeeded();
						baseIndent = currentContext().indent;
						writeToken(token);
					}
					break;
				case SqlFormatterLexer.WHERE, SqlFormatterLexer.HAVING:
					newLineIfNeeded();
					baseIndent = currentContext().indent;
					writeToken(token);
					currentContext().firstClauseElement = true;
					break;
				case SqlFormatterLexer.JOIN,
					SqlFormatterLexer.LEFT,
					SqlFormatterLexer.RIGHT,
					SqlFormatterLexer.INNER,
					SqlFormatterLexer.OUTER,
					SqlFormatterLexer.FULL,
					SqlFormatterLexer.CROSS:
					// Check if this is part of "LEFT JOIN" etc.
					Token prev = peekBack(1);
					if (type == SqlFormatterLexer.JOIN &&
						(prev != null && (prev.getType() == SqlFormatterLexer.LEFT ||
						prev.getType() == SqlFormatterLexer.RIGHT ||
						prev.getType() == SqlFormatterLexer.INNER ||
						prev.getType() == SqlFormatterLexer.OUTER ||
						prev.getType() == SqlFormatterLexer.FULL ||
						prev.getType() == SqlFormatterLexer.CROSS))) {
						space();
					}
					else {
						newLineIfNeeded();
						baseIndent = currentContext().indent;
					}
					writeToken(token);
					break;
				case SqlFormatterLexer.LATERAL,
					SqlFormatterLexer.CONFLICT,
					SqlFormatterLexer.DUPLICATE,
					SqlFormatterLexer.KEY,
					SqlFormatterLexer.MATCHED,
					SqlFormatterLexer.NOTHING,
					SqlFormatterLexer.CONSTRAINT:
					space();
					writeToken(token);
					break;
				case SqlFormatterLexer.THEN:
					processThen(token);
					break;
				case SqlFormatterLexer.ELSE:
					processElse(token);
					break;
				case SqlFormatterLexer.NOT:
					processNot(token);
					break;
				case SqlFormatterLexer.EQ:
					processEquals(token);
					break;
				case SqlFormatterLexer.AND,
					SqlFormatterLexer.OR:
					if (isBetweenAnd()) {
						space();
						writeToken(token);
					}
					else {
						newLineIfNeeded();
						baseIndent = currentContext().indent + 1;
						writeToken(token);
					}
					break;
				case SqlFormatterLexer.BY:
					space();
					writeToken(token);
					// Check if list is multi-line
					int[] terminators = (peekBack(1) != null && peekBack(1).getType() == SqlFormatterLexer.ORDER)
							? new int[]{ SqlFormatterLexer.OFFSET, SqlFormatterLexer.FETCH, SqlFormatterLexer.LIMIT }
							: new int[]{ SqlFormatterLexer.HAVING, SqlFormatterLexer.ORDER, SqlFormatterLexer.OFFSET };
					if (shouldListBeMultiline(terminators)) {
						newLine();
						baseIndent = currentContext().indent + 1;
						currentContext().firstClauseElement = true;
					}
					break;
				case SqlFormatterLexer.COMMA:
					writeToken(token);
					// Check if we should newline after comma
					Token prevToken = peekBack(1);
					if (currentContext().firstClauseElement || baseIndent > currentContext().indent) {
						// Multi-line list - add newline
						newLine();
						currentContext().firstClauseElement = false;
					}
					else if (prevToken != null && prevToken.getType() == SqlFormatterLexer.RPAREN && currentContext().type == ContextType.MAIN) {
						// CTE list: ) , next_cte ...
						// But NOT in UPDATE SET: VALUES(col), col = ...
						// Check if we're in UPDATE SET by looking for SET keyword before this
						if (!isAfterKeyword(SqlFormatterLexer.SET, 10) && !isAfterKeyword(SqlFormatterLexer.UPDATE, 15)) {
							newLine();
						}
					}
					break;
				case SqlFormatterLexer.LPAREN:
					processLeftParen(token);
					break;
				case SqlFormatterLexer.RPAREN:
					processRightParen(token);
					break;
				case SqlFormatterLexer.CASE:
					processCaseKeyword(token);
					break;
				case SqlFormatterLexer.END:
					if (inContext(ContextType.CASE_EXPR) && isCaseEnd()) {
						// END should be at the same level as CASE
						// CASE_EXPR context has indent for WHEN/ELSE (CASE + 1)
						// So CASE level is currentContext().indent - 1
						int caseIndent = currentContext().indent - 1;
						contextStack.pop();
						newLineIfNeeded();
						baseIndent = caseIndent;
					}
					writeToken(token);
					break;
				case SqlFormatterLexer.SEMICOLON:
					// If we're ending a MERGE WHEN THEN block, pop the temporary context
					if (inMergeWhenThenBlock) {
						contextStack.pop();
						inMergeWhenThenBlock = false;
					}
					writeToken(token);
					break;
				default:
					writeToken(token);
					break;
			}
		}

		private void processLineComment(Token token) {
			// Line comment - ensure it's on its own line
			// If we're at the clause level and not at the first element, indent at condition level
			int commentIndent = baseIndent;
			if (baseIndent == currentContext().indent && !currentContext().firstClauseElement) {
				commentIndent = currentContext().indent + 1;
			}
			newLineIfNeeded();
			// Apply proper indentation
			if (atLineStart) {
				indent(commentIndent);
				atLineStart = false;
			}
			output.append(token.getText());
			newLine();
		}

		private void processBlockComment(Token token) {
			// Block comment - handle special cases
			String commentText = token.getText();

			// Special case: hint comments /*+ ... */ are preserved exactly as-is
			if (commentText.startsWith("/*+")) {
				output.append(commentText);
				return;
			}

			// Multi-line comments: put on own line and carry over indentation to continuation lines
			if (commentText.contains("\n")) {
				// Ensure comment starts on its own line with proper indentation
				newLineIfNeeded();

				// Apply indentation for the first line
				if (atLineStart) {
					indent(baseIndent);
					atLineStart = false;
				}

				// Split comment into lines and re-indent continuation lines to match first line
				String[] lines = commentText.split("\n", -1);
				for (int i = 0; i < lines.length; i++) {
					if (i == 0) {
						// First line - write as-is
						output.append(lines[i]);
					}
					else {
						// Continuation lines - add newline, apply same indentation as first line, strip leading whitespace from content
						output.append("\n");
						indent(baseIndent);
						// Strip leading whitespace from continuation line
						output.append(lines[i].stripLeading());
					}
				}
				// Add newline after comment
				newLine();
			}
			else {
				// Single-line block comment - put on own line
				newLineIfNeeded();
				writeToken(token);
				newLine();
			}
		}

		private void processLeftParen(Token token) {
			Token prev = peekBack(1);

			// Determine context type
			ContextType newContextType = ContextType.FUNCTION; // Default: function call
			boolean isParenthesizedList = false;

			if (prev != null) {
				int prevType = prev.getType();

				// Check for VALUES tuple or INSERT column list
				if (prevType == SqlFormatterLexer.VALUES) {
					// Check if VALUES is a function (after =) or a VALUES clause
					Token beforeValues = peekBack(2);
					if (beforeValues != null && beforeValues.getType() == SqlFormatterLexer.EQ) {
						// VALUES(col) function in UPDATE - treat as regular function
						newContextType = ContextType.FUNCTION;
						isParenthesizedList = false;
					}
					else {
						// VALUES (tuple) clause - newline and indent, but keep content inline
						isParenthesizedList = true;
						newContextType = ContextType.FUNCTION;
					}
				}
				else if (prevType == SqlFormatterLexer.IDENTIFIER && isAfterKeyword(SqlFormatterLexer.INTO, 3)) {
					// INSERT INTO table (columns) - newline and indent, but keep content inline
					isParenthesizedList = true;
					newContextType = ContextType.FUNCTION;
				}
				else if (prevType == SqlFormatterLexer.ON && isAfterKeyword(SqlFormatterLexer.MERGE, 10)) {
					// MERGE ... ON (condition) - newline and indent, but keep content inline
					isParenthesizedList = true;
					newContextType = ContextType.FUNCTION;
				}
				else if (prevType == SqlFormatterLexer.AS) {
					// CTE or subquery after AS
					newContextType = ContextType.CTE;
				}
				else if (prevType == SqlFormatterLexer.LATERAL) {
					newContextType = ContextType.SUBQUERY;
				}
				else if (prevType == SqlFormatterLexer.EXISTS || prevType == SqlFormatterLexer.IN ||
						prevType == SqlFormatterLexer.ALL || prevType == SqlFormatterLexer.ANY) {
					// Check if these operators have a subquery or value list
					Token next = peekAhead(1);
					if (next != null && next.getType() == SqlFormatterLexer.SELECT) {
						// Operator with subquery: EXISTS (SELECT ...), IN (SELECT ...), etc.
						newContextType = ContextType.SUBQUERY;
					}
					else {
						// Operator with value list: IN ('val1', 'val2'), ALL (vals), etc.
						// Treat as operator list to handle multi-line formatting
						newContextType = ContextType.OPERATOR_LIST;
					}
				}
				else if (prevType == SqlFormatterLexer.EQ) {
					// Assignment with subquery: SET col = (SELECT ...)
					Token next = peekAhead(1);
					if (next != null && next.getType() == SqlFormatterLexer.SELECT) {
						newContextType = ContextType.SUBQUERY;
					}
				}
				else if (prevType == SqlFormatterLexer.IDENTIFIER || isKeyword(prevType)) {
					// Check if next is SELECT (subquery) or not (function)
					Token next = peekAhead(1);
					if (next != null && next.getType() == SqlFormatterLexer.SELECT) {
						newContextType = ContextType.SUBQUERY;
					}
				}
			}

			// For parenthesized lists (INSERT columns, VALUES tuples, MERGE ON conditions), newline before (
			if (isParenthesizedList) {
				newLine();
				baseIndent = currentContext().indent + 1;
			}

			// Write the token
			writeToken(token);

			// Handle context based on type
			if (newContextType == ContextType.FUNCTION) {
				// Function call or parenthesized list - stay on same line
				contextStack.push(new Context(ContextType.FUNCTION, baseIndent));
			}
			else if (newContextType == ContextType.OPERATOR_LIST) {
				// Operator list (IN, ALL, ANY) - check if multi-line
				// Check if list should be multi-line (>3 items)
				if (shouldOperatorListBeMultiline()) {
					Context ctx = new Context(ContextType.OPERATOR_LIST, baseIndent + 1, true, false);
					contextStack.push(ctx);
					newLine();
					baseIndent++;
				}
				else {
					// Single-line list
					contextStack.push(new Context(ContextType.OPERATOR_LIST, baseIndent));
				}
			}
			else {
				// Subquery/CTE - newline and indent
				newLine();
				contextStack.push(new Context(newContextType, baseIndent + 1));
				baseIndent = currentContext().indent;
			}
		}

		private void processRightParen(Token token) {
			if (!contextStack.isEmpty() && contextStack.size() > 1) {
				Context ctx = contextStack.pop();

				// For subqueries/CTEs/operator lists, dedent before closing paren
				if (ctx.type != ContextType.FUNCTION) {
					if (ctx.type == ContextType.OPERATOR_LIST) {
						// Operator list - only newline if it was explicitly made multi-line
						if (ctx.isMultiline) {
							baseIndent = currentContext().indent;
							newLineIfNeeded();
						}
						// else: single-line list, no newline needed
					}
					else {
						// Subqueries/CTEs always newline and dedent
						// The closing paren should be at the same level as where the subquery was opened
						// Since context was pushed with indent = baseIndent + 1, we dedent to ctx.indent - 1
						baseIndent = ctx.indent - 1;
						newLineIfNeeded();
					}
				}
			}

			writeToken(token);
		}

		private void processSetClause(Token token) {
			// SET can appear in UPDATE or after DO UPDATE
			// Check if the assignment list should be multiline
			// For SET, we check from current position to WHERE/FROM/RETURNING/etc
			int[] terminators = new int[]{
					SqlFormatterLexer.WHERE, SqlFormatterLexer.FROM, SqlFormatterLexer.RETURNING,
					SqlFormatterLexer.ON, SqlFormatterLexer.WHEN, SqlFormatterLexer.SEMICOLON
			};

			// Always newline before SET
			newLineIfNeeded();
			if (shouldListBeMultiline(terminators)) {
				// Multi-line SET: newline after SET keyword
				newLineIfNeeded();
				baseIndent = currentContext().indent;
				writeToken(token);
				newLine();
				baseIndent = currentContext().indent + 1;
				currentContext().firstClauseElement = true;
			}
			else {
				// Single assignment: SET stays inline with assignment
				space();
				writeToken(token);
			}
		}

		private void processOnKeyword(Token token) {
			// ON can be:
			// 1. JOIN ... ON ... (JOIN condition - indented +1)
			// 2. ON CONFLICT (PostgreSQL - major clause)
			// 3. ON DUPLICATE KEY (MySQL - major clause)
			// 4. MERGE ... ON ... (MERGE condition - at MERGE level)
			// 5. ON CONSTRAINT (after ON CONFLICT - inline)

			Token prev = peekBack(1);
			Token next = peekAhead(1);

			// Check if this is "ON CONSTRAINT" after "ON CONFLICT"
			if (prev != null && prev.getType() == SqlFormatterLexer.CONFLICT &&
					next != null && next.getType() == SqlFormatterLexer.CONSTRAINT) {
				// ON CONSTRAINT after ON CONFLICT - just add space
				space();
				writeToken(token);
			}
			else if (next != null && (next.getType() == SqlFormatterLexer.CONFLICT ||
					next.getType() == SqlFormatterLexer.DUPLICATE)) {
				// ON CONFLICT or ON DUPLICATE KEY - major clause
				newLineIfNeeded();
				baseIndent = currentContext().indent;
				writeToken(token);
			}
			else if (isAfterKeyword(SqlFormatterLexer.USING, 5) || isAfterKeyword(SqlFormatterLexer.MERGE, 10)) {
				// MERGE ... ON - at MERGE level (not indented)
				newLineIfNeeded();
				baseIndent = currentContext().indent;
				writeToken(token);
			}
			else {
				// JOIN ON - indent +1
				newLineIfNeeded();
				baseIndent = currentContext().indent + 1;
				writeToken(token);
				currentContext().firstClauseElement = true;
			}
		}

		private void processDo(Token token) {
			// DO UPDATE or DO NOTHING (PostgreSQL ON CONFLICT)
			// DO is part of ON CONFLICT clause, add newline before it
			newLineIfNeeded();
			baseIndent = currentContext().indent;
			writeToken(token);
			// UPDATE or NOTHING should stay on same line after DO
		}

		private void processWhen(Token token) {
			// WHEN can be:
			// 1. CASE ... WHEN ... (CASE expression)
			// 2. MERGE ... WHEN MATCHED/NOT MATCHED (MERGE statement)

			// If we were in a MERGE WHEN THEN block, pop the temporary context
			if (inMergeWhenThenBlock) {
				contextStack.pop();
				inMergeWhenThenBlock = false;
			}

			if (inContext(ContextType.CASE_EXPR)) {
				// CASE expression: WHEN at CASE indent level
				newLineIfNeeded();
				baseIndent = currentContext().indent;
				writeToken(token);
			}
			else {
				// MERGE WHEN: major clause
				newLineIfNeeded();
				baseIndent = currentContext().indent;
				writeToken(token);
			}
		}

		private void processElse(Token token) {
			// ELSE can appear in:
			// 1. CASE expressions (needs newline)
			// 2. Other contexts (just space)

			if (inContext(ContextType.CASE_EXPR)) {
				// CASE expression: ELSE at CASE indent level (same as WHEN)
				newLineIfNeeded();
				baseIndent = currentContext().indent;
				writeToken(token);
			}
			else {
				// Other contexts: just add space
				space();
				writeToken(token);
			}
		}

		private void processThen(Token token) {
			// THEN can appear in:
			// 1. CASE WHEN ... THEN (CASE expression)
			// 2. MERGE WHEN MATCHED THEN / WHEN NOT MATCHED THEN (MERGE statement)

			Token prev = peekBack(1);

			// Check if this is THEN after WHEN MATCHED or WHEN NOT MATCHED (in MERGE)
			if (prev != null && prev.getType() == SqlFormatterLexer.MATCHED) {
				// WHEN MATCHED THEN or WHEN NOT MATCHED THEN
				space();
				writeToken(token);
				newLine();
				// Push a new context with +1 indent for statements inside THEN block (UPDATE, INSERT, etc.)
				contextStack.push(new Context(ContextType.MAIN, currentContext().indent + 1));
				inMergeWhenThenBlock = true;
			}
			else if (inContext(ContextType.CASE_EXPR)) {
				// CASE WHEN ... THEN
				space();
				writeToken(token);
			}
			else {
				// Other contexts
				space();
				writeToken(token);
			}
		}

		private void processCaseKeyword(Token token) {
			// CASE can appear:
			// 1. In SELECT list (with other items or alone)
			// 2. In assignment (UPDATE SET col = CASE ...)
			// 3. Standalone

			// Check if we're after an equals sign (assignment context)
			Token prev = peekBack(1);
			if (prev != null && prev.getType() == SqlFormatterLexer.EQ) {
				// Assignment: newline, then CASE at +1 indent
				newLine();
				baseIndent = currentContext().indent + 1;
			}

			writeToken(token);
			contextStack.push(new Context(ContextType.CASE_EXPR, baseIndent + 1));
		}

		private void processNot(Token token) {
			// NOT can be:
			// 1. Part of "NOT MATCHED" in MERGE
			// 2. Logical NOT operator
			// 3. Part of "IS NOT NULL"

			Token next = peekAhead(1);
			if (next != null && next.getType() == SqlFormatterLexer.MATCHED) {
				// MERGE "WHEN NOT MATCHED"
				space();
				writeToken(token);
			}
			else {
				// Regular NOT operator or IS NOT
				space();
				writeToken(token);
			}
		}

		private void processEquals(Token token) {
			// Equals sign in assignment context
			// Check if followed by subquery or CASE
			Token next = peekAhead(1);

			if (next != null && (next.getType() == SqlFormatterLexer.LPAREN || next.getType() == SqlFormatterLexer.CASE)) {
				// Check if the LPAREN is followed by SELECT (subquery)
				if (next.getType() == SqlFormatterLexer.LPAREN) {
					Token afterParen = peekAhead(2);
					if (afterParen != null && afterParen.getType() == SqlFormatterLexer.SELECT) {
						// Subquery assignment: = (SELECT ...)
						writeToken(token);
						space();
						return;
					}
				}
				else if (next.getType() == SqlFormatterLexer.CASE) {
					// CASE assignment: = CASE ...
					// The processCaseKeyword will handle the newline
					writeToken(token);
					return;
				}
			}

			// Default: just write with spacing
			writeToken(token);
		}

		private void writeToken(Token token) {
			final String text = token.getText();
			final int type = token.getType();

			// Apply indentation if at line start
			if (atLineStart) {
				indent(baseIndent);
				atLineStart = false;
			}
			// Add space if needed
			else if (needsSpaceBefore(token)) {
				space();
			}

			// Write token with proper casing
			if (isKeyword(type)) {
				output.append(text.toUpperCase());
			}
			else if (type == SqlFormatterLexer.IDENTIFIER) {
				// Check if it's a function (followed by LPAREN) or a table/column name
				Token next = peekAhead(1);
				Token prev = peekBack(1);

				// Don't uppercase table names after INTO, UPDATE, DELETE, MERGE, FROM, JOIN
				if (prev != null && (prev.getType() == SqlFormatterLexer.INTO ||
						prev.getType() == SqlFormatterLexer.UPDATE ||
						prev.getType() == SqlFormatterLexer.DELETE ||
						prev.getType() == SqlFormatterLexer.MERGE ||
						prev.getType() == SqlFormatterLexer.FROM ||
						prev.getType() == SqlFormatterLexer.JOIN ||
						prev.getType() == SqlFormatterLexer.USING)) {
					output.append(text);
				}
				// Check if it's a function (followed by LPAREN and not after INTO/VALUES)
				else if (next != null && next.getType() == SqlFormatterLexer.LPAREN &&
						(prev == null || (prev.getType() != SqlFormatterLexer.INTO &&
								prev.getType() != SqlFormatterLexer.VALUES))) {
					output.append(text.toUpperCase());
				}
				else {
					output.append(text);
				}
			}
			else {
				output.append(text);
			}

			// Reset first element flag
			if (currentContext().firstClauseElement && !isLogical(type) && type != SqlFormatterLexer.COMMA) {
				currentContext().firstClauseElement = false;
			}
		}

		private boolean needsSpaceBefore(Token token) {
			final int type = token.getType();

			// No space before these
			if (type == SqlFormatterLexer.COMMA || type == SqlFormatterLexer.RPAREN ||
				type == SqlFormatterLexer.DOT || type == SqlFormatterLexer.SEMICOLON) {
				return false;
			}

			// Check last character
			if (output.isEmpty()) {
				return false;
			}

			final char lastChar = output.charAt(output.length() - 1);
			if (lastChar == ' ' || lastChar == '(' || lastChar == '\n' || lastChar == '\t') {
				return false;
			}

			// No space after opening paren or dot
			Token prev = peekBack(1);
			if (prev != null) {
				int prevType = prev.getType();
				if (prevType == SqlFormatterLexer.LPAREN || prevType == SqlFormatterLexer.DOT) {
					return false;
				}
			}

			// No space before opening paren after identifier/keyword (function call)
			// EXCEPT for certain keywords that require a space before (
			if (type == SqlFormatterLexer.LPAREN && prev != null) {
				int prevType = prev.getType();
				// These keywords need space before (: clause keywords, operators, etc.
				if (prevType == SqlFormatterLexer.SELECT || prevType == SqlFormatterLexer.LATERAL ||
					prevType == SqlFormatterLexer.EXISTS || prevType == SqlFormatterLexer.IN ||
					prevType == SqlFormatterLexer.ALL || prevType == SqlFormatterLexer.ANY ||
					prevType == SqlFormatterLexer.CONFLICT || prevType == SqlFormatterLexer.ON ||
					prevType == SqlFormatterLexer.INSERT) {
					return true;
				}
				// All other identifiers/keywords don't need space
				return prevType != SqlFormatterLexer.IDENTIFIER && !isKeyword( prevType );
			}

			return true;
		}

		private boolean isKeyword(int type) {
			return type >= SqlFormatterLexer.SELECT && type <= SqlFormatterLexer.CONTINUE;
		}

		private boolean isAfterKeyword(int keywordType, int lookback) {
			for (int i = position - 1; i >= 0 && i >= position - lookback; i--) {
				Token t = tokens.get(i);
				if (t.getType() == keywordType) {
					return true;
				}
			}
			return false;
		}

		private boolean isLogical(int type) {
			return type == SqlFormatterLexer.AND || type == SqlFormatterLexer.OR;
		}

		private boolean isBetweenAnd() {
			for (int i = position - 1; i >= 0 && i >= position - 10; i--) {
				Token t = tokens.get(i);
				if (t.getType() == SqlFormatterLexer.BETWEEN) {
					return true;
				}
				if (t.getType() == SqlFormatterLexer.SELECT || t.getType() == SqlFormatterLexer.FROM ||
					t.getType() == SqlFormatterLexer.WHERE || t.getType() == SqlFormatterLexer.HAVING) {
					break;
				}
			}
			return false;
		}

		private boolean isCaseEnd() {
			int caseCount = 0;
			int endCount = 0;
			for (int i = position - 1; i >= 0; i--) {
				Token t = tokens.get(i);
				if (t.getType() == SqlFormatterLexer.CASE) {
					caseCount++;
				}
				else if (t.getType() == SqlFormatterLexer.END) {
					endCount++;
				}
				if (caseCount > endCount) {
					return true;
				}
			}
			return false;
		}

		private boolean inContext(ContextType type) {
			for (Context ctx : contextStack) {
				if (ctx.type == type) {
					return true;
				}
			}
			return false;
		}

		private Context currentContext() {
			return contextStack.peek();
		}

		private boolean shouldListBeMultiline(int... terminators) {
			int commaCount = 0;
			int parenDepth = 0;

			for (int i = position + 1; i < tokens.size(); i++) {
				Token t = tokens.get(i);
				int type = t.getType();

				if (type == SqlFormatterLexer.LPAREN) {
					parenDepth++;
				}
				else if (type == SqlFormatterLexer.RPAREN) {
					parenDepth--;
					if (parenDepth < 0) {
						break;
					}
				}
				else if (parenDepth == 0) {
					if (type == SqlFormatterLexer.COMMA) {
						commaCount++;
					}
					else {
						for (int term : terminators) {
							if (type == term) {
								// List has (commaCount + 1) items
								return commaCount + 1 > 3;
							}
						}
					}
				}
			}

			return commaCount > 0;
		}

		private boolean shouldOperatorListBeMultiline() {
			// Check if operator list (IN, ALL, ANY) has >3 items
			int commaCount = 0;
			int parenDepth = 0;

			for (int i = position + 1; i < tokens.size(); i++) {
				Token t = tokens.get(i);
				int type = t.getType();

				if (type == SqlFormatterLexer.LPAREN) {
					parenDepth++;
				}
				else if (type == SqlFormatterLexer.RPAREN) {
					if (parenDepth == 0) {
						// Found matching closing paren
						// List has (commaCount + 1) items
						return commaCount + 1 > 3;
					}
					parenDepth--;
				}
				else if (parenDepth == 0 && type == SqlFormatterLexer.COMMA) {
					commaCount++;
				}
			}

			return false;
		}

		private Token currentToken() {
			return tokens.get(position);
		}

		private Token peekAhead(int offset) {
			int pos = position + offset;
			return pos < tokens.size() ? tokens.get(pos) : null;
		}

		private Token peekBack(int offset) {
			int pos = position - offset;
			return pos >= 0 ? tokens.get(pos) : null;
		}

		private void newLine() {
			if (!atLineStart) {
				output.append(LINE_SEPARATOR);
				atLineStart = true;
			}
		}

		private void newLineIfNeeded() {
			if (!atLineStart && !output.isEmpty()) {
				newLine();
			}
		}

		private void space() {
			if (!output.isEmpty()) {
				final char lastChar = output.charAt(output.length() - 1);
				if (lastChar != ' ' && lastChar != '\t' && lastChar != '\n') {
					output.append(' ');
				}
			}
		}

		private void indent(int indentCount) {
			output.append( INDENT.repeat( indentCount ) );
		}
	}

}
