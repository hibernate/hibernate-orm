// Generated from /home/sebersole/projects/hibernate-orm/wip-6/hibernate-core/src/main/antlr/org/hibernate/grammars/importsql/SqlScriptLexer.g4 by ANTLR 4.9.1

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.grammars.importsql;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SqlScriptLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		LINE_COMMENT=1, MULTILINE_COMMENT=2, CHAR=3, SPACE=4, TAB=5, NEWLINE=6, 
		DELIMITER=7, QUOTED_TEXT=8;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"LINE_COMMENT", "MULTILINE_COMMENT", "CHAR", "SPACE", "TAB", "NEWLINE", 
			"DELIMITER", "QUOTED_TEXT"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, "' '", "'\t'", null, "';'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "LINE_COMMENT", "MULTILINE_COMMENT", "CHAR", "SPACE", "TAB", "NEWLINE", 
			"DELIMITER", "QUOTED_TEXT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public SqlScriptLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "SqlScriptLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	@Override
	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 1:
			return MULTILINE_COMMENT_sempred((RuleContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean MULTILINE_COMMENT_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return  getInputStream().LA(2)!='/' ;
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\nW\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\3\2\3\2\3\2\3\2"+
		"\5\2\30\n\2\3\2\7\2\33\n\2\f\2\16\2\36\13\2\3\2\3\2\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\7\3+\n\3\f\3\16\3.\13\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4"+
		"\3\5\3\5\3\6\3\6\3\7\5\7<\n\7\3\7\3\7\5\7@\n\7\3\b\3\b\3\t\3\t\7\tF\n"+
		"\t\f\t\16\tI\13\t\3\t\3\t\3\t\3\t\3\t\7\tP\n\t\f\t\16\tS\13\t\3\t\5\t"+
		"V\n\t\4GQ\2\n\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\3\2\7\4\2\f\f\17\17\5"+
		"\2\f\f\17\17,,\6\2\13\f\17\17\"\"==\3\2bb\3\2))\2b\2\3\3\2\2\2\2\5\3\2"+
		"\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21"+
		"\3\2\2\2\3\27\3\2\2\2\5!\3\2\2\2\7\64\3\2\2\2\t\66\3\2\2\2\138\3\2\2\2"+
		"\r?\3\2\2\2\17A\3\2\2\2\21U\3\2\2\2\23\24\7\61\2\2\24\30\7\61\2\2\25\26"+
		"\7/\2\2\26\30\7/\2\2\27\23\3\2\2\2\27\25\3\2\2\2\30\34\3\2\2\2\31\33\n"+
		"\2\2\2\32\31\3\2\2\2\33\36\3\2\2\2\34\32\3\2\2\2\34\35\3\2\2\2\35\37\3"+
		"\2\2\2\36\34\3\2\2\2\37 \b\2\2\2 \4\3\2\2\2!\"\7\61\2\2\"#\7,\2\2#,\3"+
		"\2\2\2$%\6\3\2\2%+\7,\2\2&\'\7\17\2\2\'+\7\f\2\2(+\t\2\2\2)+\n\3\2\2*"+
		"$\3\2\2\2*&\3\2\2\2*(\3\2\2\2*)\3\2\2\2+.\3\2\2\2,*\3\2\2\2,-\3\2\2\2"+
		"-/\3\2\2\2.,\3\2\2\2/\60\7,\2\2\60\61\7\61\2\2\61\62\3\2\2\2\62\63\b\3"+
		"\2\2\63\6\3\2\2\2\64\65\n\4\2\2\65\b\3\2\2\2\66\67\7\"\2\2\67\n\3\2\2"+
		"\289\7\13\2\29\f\3\2\2\2:<\7\17\2\2;:\3\2\2\2;<\3\2\2\2<=\3\2\2\2=@\7"+
		"\f\2\2>@\7\17\2\2?;\3\2\2\2?>\3\2\2\2@\16\3\2\2\2AB\7=\2\2B\20\3\2\2\2"+
		"CG\7b\2\2DF\n\5\2\2ED\3\2\2\2FI\3\2\2\2GH\3\2\2\2GE\3\2\2\2HJ\3\2\2\2"+
		"IG\3\2\2\2JV\7b\2\2KQ\7)\2\2LM\7)\2\2MP\7)\2\2NP\n\6\2\2OL\3\2\2\2ON\3"+
		"\2\2\2PS\3\2\2\2QR\3\2\2\2QO\3\2\2\2RT\3\2\2\2SQ\3\2\2\2TV\7)\2\2UC\3"+
		"\2\2\2UK\3\2\2\2V\22\3\2\2\2\r\2\27\34*,;?GOQU\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}