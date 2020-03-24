parser grammar SqlScriptParser;

options {
	tokenVocab=SqlScriptLexer;
}

@header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.grammars.importsql;
}

script
	: commandBlock+ EOF
	;

commandBlock
	: command STMT_END
	;

command
	: commandPart*
	;

commandPart
	: notStmtEnd
	| quotedText
	;

notStmtEnd
	: NOT_STMT_END+
	;

quotedText
	: QUOTED_TEXT
	;

