/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.community.dialect.function.GaussDBFormatFunction;
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

	public GaussDBFunctionRegistry(FunctionContributions functionContributions) {
		this.functionContributions = functionContributions;
		this.functionRegistry = functionContributions.getFunctionRegistry();
		this.typeConfiguration = functionContributions.getTypeConfiguration();
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
		functionFactory.stddevPopSamp();
		functionFactory.variance();
		functionFactory.varPopSamp();
		functionFactory.covarPopSamp();
		functionFactory.corr();
		functionFactory.regrLinearRegressionAggregates();
		functionFactory.insert_overlay();
		functionFactory.overlay();
		functionFactory.soundex(); //was introduced apparently
		functionFactory.locate_positionSubstring();
		functionFactory.windowFunctions();
		functionFactory.listagg_stringAgg( "varchar" );
		functionFactory.arrayAggregate();
		functionFactory.arraySlice_operator();
		functionFactory.makeDateTimeTimestamp();
		// Note that GaussDB doesn't support the OVER clause for ordered set-aggregate functions
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates();
		functionFactory.dateTrunc();
		functionFactory.hex( "encode(?1, 'hex')" );
		functionFactory.sha( "sha256(?1)" );
		functionFactory.md5( "decode(md5(?1), 'hex')" );

		functionContributions.getFunctionRegistry().register( "min", new GaussDBMinMaxFunction( "min" ) );
		functionContributions.getFunctionRegistry().register( "max", new GaussDBMinMaxFunction( "max" ) );

		// uses # instead of ^ for XOR
		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "bitxor", "(?1 # ?2)" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionContributions.getFunctionRegistry().register(
				"round", new GaussDBTruncRoundFunction( "round", true )
		);
		functionContributions.getFunctionRegistry().register(
				"trunc",
				new GaussDBTruncFunction( true, functionContributions.getTypeConfiguration() )
		);
		functionContributions.getFunctionRegistry().registerAlternateKey( "truncate", "trunc" );

		format_toChar_gaussdb();
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
	}

	public void format_toChar_gaussdb() {
		functionRegistry.register( "format", new GaussDBFormatFunction( "to_char", typeConfiguration ) );
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
		functionRegistry.register( "json_object", new GaussDBJsonObjectFunction( typeConfiguration ) );
	}
}
