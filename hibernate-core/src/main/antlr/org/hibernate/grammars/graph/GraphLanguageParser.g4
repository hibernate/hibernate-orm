parser grammar GraphLanguageParser;

options {
	tokenVocab=GraphLanguageLexer;
}

@header {
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.grammars.graph;
}

@members {
/*
 * Antlr grammar describing the Hibernate EntityGraph Language - for parsing a structured
 * textual representation of an entity graph
 *
 * `GraphLanguageParser.g4`
 */
}


graph
 	: attributeList
 	;

attributeList
	: attributeNode (COMMA attributeNode)*
    ;

attributeNode
	: attributePath subGraph?
	;

attributePath
	: ATTR_NAME attributeQualifier?
	;

attributeQualifier
	: DOT ATTR_NAME
	;

subGraph
	: LPAREN (subType COLON)? attributeList RPAREN
	;

subType
	: TYPE_NAME
	;
