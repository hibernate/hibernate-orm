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

/**
 * Advanced SQL formatter using ANTLR-based tokenization.
 * Provides better formatting for complex SQL queries compared to BasicFormatterImpl.
 *
 * @author Hibernate Team
 */
public class TokenBasedFormatterImpl implements Formatter {

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
			// Fall back to unformatted SQL if formatting fails
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

		// Context types
		private enum ContextType {
			MAIN,           // Main query
			CTE,            // Common Table Expression
			SUBQUERY,       // Subquery in FROM/JOIN
			EXISTS_SUB,     // EXISTS subquery
			FUNCTION,       // Function call
			CASE_EXPR,      // CASE expression
			OPERATOR_LIST   // Operator list (IN, ALL, etc.)
		}

		private static class Context {
			ContextType type;
			int indent;
			boolean firstClauseElement; // For first condition in WHERE/ON, first item in SELECT list, etc.
			boolean isMultiline; // For operator lists - tracks if list was made multi-line

			Context(ContextType type, int indent) {
				this.type = type;
				this.indent = indent;
				this.firstClauseElement = false;
				this.isMultiline = false;
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

			// Handle comments
			if (type == SqlFormatterLexer.LINE_COMMENT || type == SqlFormatterLexer.BLOCK_COMMENT) {
				writeToken(token);
				return;
			}

			// Handle specific token types
			switch (type) {
				case SqlFormatterLexer.WITH:
					newLineIfNeeded();
					baseIndent = currentContext().indent;
					writeToken(token);
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
					// Check if we're inside a function (like TRIM)
					if (currentContext().type == ContextType.FUNCTION) {
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
				case SqlFormatterLexer.WHERE:
				case SqlFormatterLexer.HAVING:
					newLineIfNeeded();
					baseIndent = currentContext().indent;
					writeToken(token);
					currentContext().firstClauseElement = true;
					break;
				case SqlFormatterLexer.JOIN:
				case SqlFormatterLexer.LEFT:
				case SqlFormatterLexer.RIGHT:
				case SqlFormatterLexer.INNER:
				case SqlFormatterLexer.OUTER:
				case SqlFormatterLexer.FULL:
				case SqlFormatterLexer.CROSS:
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
				case SqlFormatterLexer.LATERAL:
					space();
					writeToken(token);
					break;
				case SqlFormatterLexer.ON:
					newLineIfNeeded();
					baseIndent = currentContext().indent + 1;
					writeToken(token);
					currentContext().firstClauseElement = true;
					break;
				case SqlFormatterLexer.AND:
				case SqlFormatterLexer.OR:
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
				case SqlFormatterLexer.ORDER:
				case SqlFormatterLexer.GROUP:
					newLineIfNeeded();
					baseIndent = currentContext().indent;
					writeToken(token);
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
					if (currentContext().firstClauseElement || baseIndent > currentContext().indent ||
						(prevToken != null && prevToken.getType() == SqlFormatterLexer.RPAREN && currentContext().type == ContextType.MAIN)) {
						// CTE comma, list item comma, etc - add newline
						newLine();
						currentContext().firstClauseElement = false;
					}
					break;
				case SqlFormatterLexer.LPAREN:
					processleftParen(token);
					break;
				case SqlFormatterLexer.RPAREN:
					processRightParen(token);
					break;
				case SqlFormatterLexer.OFFSET:
				case SqlFormatterLexer.FETCH:
				case SqlFormatterLexer.LIMIT:
					newLineIfNeeded();
					baseIndent = currentContext().indent;
					writeToken(token);
					break;
				case SqlFormatterLexer.CASE:
					writeToken(token);
					contextStack.push(new Context(ContextType.CASE_EXPR, baseIndent + 1));
					break;
				case SqlFormatterLexer.WHEN:
					if (inContext(ContextType.CASE_EXPR)) {
						newLineIfNeeded();
						baseIndent = currentContext().indent;
					}
					writeToken(token);
					break;
				case SqlFormatterLexer.THEN:
				case SqlFormatterLexer.ELSE:
					space();
					writeToken(token);
					break;
				case SqlFormatterLexer.END:
					if (inContext(ContextType.CASE_EXPR) && isCaseEnd()) {
						contextStack.pop();
						newLineIfNeeded();
						baseIndent = currentContext().indent;
					}
					writeToken(token);
					break;
				case SqlFormatterLexer.UNION:
				case SqlFormatterLexer.INTERSECT:
				case SqlFormatterLexer.EXCEPT:
					newLineIfNeeded();
					baseIndent = currentContext().indent;
					writeToken(token);
					break;
				default:
					writeToken(token);
					break;
			}
		}

		private void processleftParen(Token token) {
			Token prev = peekBack(1);

			// Determine context type
			ContextType newContextType = ContextType.FUNCTION; // Default: function call

			if (prev != null) {
				int prevType = prev.getType();
				if (prevType == SqlFormatterLexer.AS) {
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
						newContextType = (prevType == SqlFormatterLexer.EXISTS)
								? ContextType.EXISTS_SUB
								: ContextType.SUBQUERY;
					}
					else {
						// Operator with value list: IN ('val1', 'val2'), ALL (vals), etc.
						// Treat as operator list to handle multi-line formatting
						newContextType = ContextType.OPERATOR_LIST;
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

			// Write the token
			writeToken(token);

			// Handle context based on type
			if (newContextType == ContextType.FUNCTION) {
				// Function call - stay on same line
				contextStack.push(new Context(ContextType.FUNCTION, baseIndent));
			}
			else if (newContextType == ContextType.OPERATOR_LIST) {
				// Operator list (IN, ALL, ANY) - check if multi-line
				// Check if list should be multi-line (>3 items)
				if (shouldOperatorListBeMultiline()) {
					Context ctx = new Context(ContextType.OPERATOR_LIST, baseIndent + 1);
					ctx.isMultiline = true;
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
						baseIndent = currentContext().indent;
						newLineIfNeeded();
					}
				}
			}

			writeToken(token);

			// Check what follows
			Token next = peekAhead(1);
			if (next != null) {
				if (next.getType() == SqlFormatterLexer.COMMA) {
					// CTE comma - stays on same line, newline happens after comma
				}
				else if (next.getType() == SqlFormatterLexer.ON) {
					// Reset indent for ON clause (already at parent level)
				}
			}
		}

		private void writeToken(Token token) {
			final String text = token.getText();
			final int type = token.getType();

			// Apply indentation if at line start
			if (atLineStart) {
				for (int i = 0; i < baseIndent; i++) {
					output.append(INDENT);
				}
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
				// Check if it's a function (followed by LPAREN)
				Token next = peekAhead(1);
				if (next != null && next.getType() == SqlFormatterLexer.LPAREN) {
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
			// EXCEPT for LATERAL, EXISTS, IN, ALL, and ANY which need a space
			if (type == SqlFormatterLexer.LPAREN && prev != null) {
				int prevType = prev.getType();
				// LATERAL, EXISTS, IN, ALL, and ANY need space before (
				if (prevType == SqlFormatterLexer.LATERAL || prevType == SqlFormatterLexer.EXISTS ||
					prevType == SqlFormatterLexer.IN || prevType == SqlFormatterLexer.ALL ||
					prevType == SqlFormatterLexer.ANY) {
					return true;
				}
				// All other identifiers/keywords don't need space
				if (prevType == SqlFormatterLexer.IDENTIFIER || isKeyword(prevType)) {
					return false;
				}
			}

			return true;
		}

		private boolean isKeyword(int type) {
			return type >= SqlFormatterLexer.SELECT && type <= SqlFormatterLexer.TRAILING;
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
	}
}
