/**
 * Test package for metatata facilities
 * It contains an example of filter metadata
 */
@FilterDefs(
		{
		@FilterDef(
				name = "betweenLength",
				defaultCondition = ":minLength <= length and :maxLength >= length",
				parameters = {
				@ParamDef(name = "minLength", type = "integer"),
				@ParamDef(name = "maxLength", type = "integer")
						}
		)
				}
)
@TypeDefs(
		{
		@TypeDef(
				name = "caster",
				typeClass = CasterStringType.class,
				parameters = {
				@Parameter(name = "cast", value = "lower")
						}
		)
				}
) package org.hibernate.test.annotations.entity;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
