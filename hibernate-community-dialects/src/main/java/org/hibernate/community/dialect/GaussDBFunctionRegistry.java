/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.community.dialect.function.GaussDBMinMaxFunction;
import org.hibernate.community.dialect.function.GaussDBTruncFunction;
import org.hibernate.community.dialect.function.GaussDBTruncRoundFunction;
import org.hibernate.community.dialect.function.array.GaussDBArrayConcatElementFunction;
import org.hibernate.community.dialect.function.array.GaussDBArrayConcatFunction;
import org.hibernate.community.dialect.function.array.GaussDBArrayConstructorFunction;
import org.hibernate.community.dialect.function.array.GaussDBArrayContainsOperatorFunction;
import org.hibernate.community.dialect.function.array.GaussDBArrayFillFunction;
import org.hibernate.community.dialect.function.array.GaussDBArrayRemoveFunction;
import org.hibernate.community.dialect.function.array.GaussDBArrayRemoveIndexFunction;
import org.hibernate.community.dialect.function.array.GaussDBArrayReplaceFunction;
import org.hibernate.community.dialect.function.array.GaussDBArraySetFunction;
import org.hibernate.community.dialect.function.json.GaussDBJsonObjectFunction;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.RegexpLikeOperatorFunction;
import org.hibernate.dialect.function.array.ArrayIncludesOperatorFunction;
import org.hibernate.dialect.function.array.ArrayIntersectsOperatorFunction;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * GaussDB functions register.
 *
 * @author liubao
 */
public class GaussDBFunctionRegistry {
	private final FunctionContributions functionContributions;

	private final SqmFunctionRegistry functionRegistry;

	private final TypeConfiguration typeConfiguration;

	private final boolean mMode;

	public GaussDBFunctionRegistry(FunctionContributions functionContributions, boolean mMode) {
		this.functionContributions = functionContributions;
		this.functionRegistry = functionContributions.getFunctionRegistry();
		this.typeConfiguration = functionContributions.getTypeConfiguration();
		this.mMode = mMode;
	}

	public void register() {
		CommonFunctionFactory functionFactory = new CommonFunctionFactory( functionContributions);
		functionFactory.cot();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.log();
		functionFactory.mod_operator();
		functionFactory.moreHyperbolic();
		functionFactory.cbrt();
		functionFactory.pi();
		functionFactory.log10_log();
		functionFactory.trim2();
		functionFactory.repeat();
		functionFactory.initcap();
		functionFactory.substr();
		functionFactory.substring_substr();
		//also natively supports ANSI-style substring()
		functionFactory.translate();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.localtimeLocaltimestamp();
		functionFactory.bitLength_pattern( "bit_length(?1)", "length(?1)*8" );
		functionFactory.octetLength_pattern( "octet_length(?1)", "length(?1)" );
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.position();
		functionFactory.bitandorxornot_operator();
		functionFactory.bitAndOr();
		functionFactory.everyAny_boolAndOr();
		functionFactory.median_percentileCont( false );
		functionFactory.stddev();
		functionFactory.covarPopSamp();
		functionFactory.corr();
		functionFactory.regrLinearRegressionAggregates();
		// GaussDB M mode (MySQL-compatible) rejects the PG-style statistical aggregates
		// stddev_pop/stddev_samp, variance and var_pop/var_samp with
		// "function() is not supported in M-format database", while it does support
		// stddev, covar_pop/covar_samp, corr and regr_*. A mode (openGauss PG kernel)
		// supports all of them, so register the unsupported ones only in A mode.
		if ( !mMode ) {
			functionFactory.stddevPopSamp();
			functionFactory.variance();
			functionFactory.varPopSamp();
		}
		// A mode (openGauss PG kernel) supports ANSI overlay() natively, so register insert via
		// overlay and overlay directly. M mode (MySQL-compatible) rejects ANSI overlay syntax; use
		// the real MySQL insert(str,start,len,repl) and let the base InsertSubstringOverlayEmulation
		// emulate overlay() through it.
		if ( mMode ) {
			functionFactory.insert();
		}
		else {
			functionFactory.insert_overlay();
			functionFactory.overlay();
		}
		functionFactory.soundex(); //was introduced apparently
		functionFactory.locate_positionSubstring();
		functionFactory.windowFunctions();
		// A mode (openGauss PG kernel) supports hypothetical-set aggregates (rank / dense_rank /
		// percent_rank / cume_dist) as both WITHIN GROUP ordered-set and window (OVER) functions.
		// M mode (MySQL-compatible) rejects the WITHIN GROUP syntax ("Function rank(...) does not
		// exist"), so register them only in A mode.
		if ( !mMode ) {
			functionFactory.hypotheticalOrderedSetAggregates();
		}
		functionFactory.listagg_stringAgg( "varchar" );
		functionFactory.arrayAggregate();
		functionFactory.arraySlice_operator();
		functionFactory.makeDateTimeTimestamp();
		// M mode (MySQL-compatible) does not support ordered-set aggregate functions with WITHIN
		// GROUP — it reports "Function rank(integer,integer) does not exist", treating the
		// within-group ORDER BY expression as a second argument; window-emulation (OVER) is also
		// unsupported. A mode supports the hypothetical-set variants (registered above). The
		// inverse-distribution variants (percentile_cont / percentile_disc / mode) are not
		// registered for either mode, so SupportsInverseDistributionFunctions returns false and
		// those tests are skipped instead of failing against unsupported syntax.
		if ( mMode ) {
			// M mode (MySQL-compatible) lacks PostgreSQL's encode/date_trunc/to_char(datetime);
			// use MySQL equivalents. format=date_format is also required by trunc's FORMAT
			// emulation (DateTruncEmulation renders str_to_date(date_format(...),...)).
			functionFactory.hex( "hex(?1)" );
			functionFactory.format_dateFormat();
			functionFactory.pad_space();
			functionFactory.trunc_truncate();
		}
		else {
			functionFactory.dateTrunc();
			functionFactory.hex( "encode(?1, 'hex')" );
			functionFactory.sha( "sha256(?1)" );
			functionFactory.md5( "decode(md5(?1), 'hex')" );
			functionFactory.format_toChar();
		}

		functionContributions.getFunctionRegistry().register( "min", new GaussDBMinMaxFunction( "min", mMode ) );
		functionContributions.getFunctionRegistry().register( "max", new GaussDBMinMaxFunction( "max", mMode ) );

		// uses # instead of ^ for XOR
		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "bitxor", "(?1 # ?2)" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionContributions.getFunctionRegistry().register(
				"round", new GaussDBTruncRoundFunction( "round", true )
		);
		if ( !mMode ) {
			// A mode: GaussDB-specific trunc (date_trunc + GaussDBTruncRoundFunction for numbers).
			// M mode: trunc_truncate() above already registered trunc (FORMAT emulation + truncate()).
			functionContributions.getFunctionRegistry().register(
					"trunc",
					new GaussDBTruncFunction( true, functionContributions.getTypeConfiguration() )
			);
		}
		functionContributions.getFunctionRegistry().registerAlternateKey( "truncate", "trunc" );

		array_gaussdb();
		arrayRemoveIndex_gaussdb();
		arrayConcat_gaussdb();
		arrayPrepend_gaussdb();
		arrayAppend_gaussdb();
		arrayContains_gaussdb();
		arrayIntersects_gaussdb();
		arrayRemove_gaussdb();
		arrayReplace_gaussdb();
		arraySet_gaussdb();
		arrayFill_gaussdb();
		jsonObject_gaussdb();
		if ( mMode ) {
			// M mode ships a builtin `regexp_like(text,text,text)` PL/pgSQL function whose 3-arg
			// form raises "CASE statement is missing ELSE part" — its body uses a PL/pgSQL CASE
			// statement without ELSE, which M mode's engine rejects even when a WHEN branch matches.
			// The 2-arg builtin (`$1 ~ $2`) works, but CommonFunctionFactory.regexpLike() routes both
			// arities through the named (builtin) function. Render with the `~`/`~*` operators
			// instead (2-arg case-sensitive, 3-arg with literal 'i' case-insensitive), which M mode
			// supports natively. A mode keeps the builtin function via the common factory.
			functionRegistry.register( "regexp_like", new RegexpLikeOperatorFunction( typeConfiguration, false ) );
		}
		else {
			functionFactory.regexpLike();
		}
	}

	public void array_gaussdb() {
		functionRegistry.register( "array", new GaussDBArrayConstructorFunction( false ) );
		functionRegistry.register( "array_list", new GaussDBArrayConstructorFunction( true ) );
	}

	public void arrayContains_gaussdb() {
		functionRegistry.register( "array_contains_nullable", new GaussDBArrayContainsOperatorFunction( true, typeConfiguration ) );
		functionRegistry.register( "array_includes", new ArrayIncludesOperatorFunction( false, typeConfiguration ) );
		functionRegistry.register( "array_includes_nullable", new ArrayIncludesOperatorFunction( true, typeConfiguration ) );
	}

	public void arrayIntersects_gaussdb() {
		functionRegistry.register( "array_intersects", new ArrayIntersectsOperatorFunction( false, typeConfiguration ) );
		functionRegistry.register( "array_intersects_nullable", new ArrayIntersectsOperatorFunction( true, typeConfiguration ) );
		functionRegistry.registerAlternateKey( "array_overlaps", "array_intersects" );
		functionRegistry.registerAlternateKey( "array_overlaps_nullable", "array_intersects_nullable" );
	}

	public void arrayConcat_gaussdb() {
		functionRegistry.register( "array_concat", new GaussDBArrayConcatFunction() );
	}

	public void arrayPrepend_gaussdb() {
		functionRegistry.register( "array_prepend", new GaussDBArrayConcatElementFunction( true ) );
	}

	public void arrayAppend_gaussdb() {
		functionRegistry.register( "array_append", new GaussDBArrayConcatElementFunction( false ) );
	}

	public void arraySet_gaussdb() {
		functionRegistry.register( "array_set", new GaussDBArraySetFunction() );
	}

	public void arrayRemove_gaussdb() {
		functionRegistry.register( "array_remove",  new GaussDBArrayRemoveFunction());
	}

	public void arrayRemoveIndex_gaussdb() {
		functionRegistry.register( "array_remove_index", new GaussDBArrayRemoveIndexFunction( false) );
	}

	public void arrayReplace_gaussdb() {
		functionRegistry.register( "array_replace", new GaussDBArrayReplaceFunction() );
	}

	public void arrayFill_gaussdb() {
		functionRegistry.register( "array_fill", new GaussDBArrayFillFunction( false ) );
		functionRegistry.register( "array_fill_list", new GaussDBArrayFillFunction( true ) );
	}

	public void jsonObject_gaussdb() {
		functionRegistry.register( "json_object", new GaussDBJsonObjectFunction( functionContributions.getDialect(), typeConfiguration ) );
	}
}
