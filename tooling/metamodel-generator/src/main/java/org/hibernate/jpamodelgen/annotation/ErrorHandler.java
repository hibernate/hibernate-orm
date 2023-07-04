/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.validation.Validation;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.BitSet;

import static org.hibernate.query.hql.internal.StandardHqlTranslator.prettifyAntlrError;

/**
 * @author Gavin King
 */
class ErrorHandler implements Validation.Handler {
	private final Element element;
	private final AnnotationMirror mirror;
	private final String queryString;
	private final Context context;
	private int errorCount;

	public ErrorHandler(Element element, AnnotationMirror mirror, String queryString, Context context) {
		this.element = element;
		this.mirror = mirror;
		this.queryString = queryString;
		this.context = context;
	}

	@Override
	public int getErrorCount() {
		return errorCount;
	}

	@Override
	public void error(int start, int end, String message) {
		errorCount++;
		context.message( element, mirror, message, Diagnostic.Kind.ERROR );
	}

	@Override
	public void warn(int start, int end, String message) {
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String message, RecognitionException e) {
		errorCount++;
		String prettyMessage = "illegal HQL syntax - "
				+ prettifyAntlrError( offendingSymbol, line, charPositionInLine, message, e, queryString, false );
		context.message( element, mirror, prettyMessage, Diagnostic.Kind.ERROR );
	}

	@Override
	public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
	}

	@Override
	public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
	}

	@Override
	public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
	}
}
