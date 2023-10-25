package org.hibernate.hql.internal.ast.util;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import org.assertj.core.api.Assertions;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.schema.ast.GeneratedSqlScriptParserTokenTypes;
import org.junit.Assert;
import org.junit.Test;

public class ASTUtilTest extends BaseUnitTestCase {

    @Test
    public void generateTokenNameCacheWithImplementator() {
        // create an instrumented interface with a synthetic field
        final DynamicType.Loaded<GeneratedSqlScriptParserTokenTypes> generatedClass = new ByteBuddy()
                .subclass(GeneratedSqlScriptParserTokenTypes.class)
                .defineField("$$customField$$", int[].class, Ownership.STATIC,
                        SyntheticState.SYNTHETIC, Visibility.PUBLIC, FieldManifestation.FINAL)
                .make()
                .load(getClass().getClassLoader());

        // compute 'normal' result
        final String[] fieldsOrigin = ASTUtil.generateTokenNameCache(GeneratedSqlScriptParserTokenTypes.class);
        // compute 'instrumented' result
        final String[] fieldsImplementator = ASTUtil.generateTokenNameCache(generatedClass.getLoaded());
        // even though size might vary, assert that known indexes are valid even with additional fields
        Assertions.assertThat(fieldsImplementator)
                .hasSizeGreaterThanOrEqualTo(fieldsOrigin.length)
                .startsWith(fieldsOrigin);
    }

    @Test
    public void generateTokenNameCache() {
        // HHH-15426 : this test failed when run with code coverage under some IDEs.
        final String[] fieldsByValue = ASTUtil.generateTokenNameCache(GeneratedSqlScriptParserTokenTypes.class);
        Assert.assertEquals("BLOCK_COMMENT", fieldsByValue[GeneratedSqlScriptParserTokenTypes.BLOCK_COMMENT]);
    }

}