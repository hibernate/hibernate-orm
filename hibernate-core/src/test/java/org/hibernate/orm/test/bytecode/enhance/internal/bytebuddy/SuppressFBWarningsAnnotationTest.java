package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import java.io.File;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

@TestForIssue(jiraKey = "HHH-15846")
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(inlineDirtyChecking = true, addSuppressFBWarnings = true, biDirectionalAssociationManagement = true, lazyLoading = true, extendedEnhancement = true)
public class SuppressFBWarningsAnnotationTest {

    @Test
    public void shouldDeclareAnnotationOnFieldsInEntityClass() {
        fields()
            .that()
            .areDeclaredIn(CardGame.class)
            .and()
            .haveNameMatching("\\$\\$_hibernate_.*")
            .should()
            .beAnnotatedWith("org.hibernate.bytecode.enhance.internal.bytebuddy.CodeTemplates$SuppressFBWarnings")
            .check(javaClasses());
    }

    @Test
    public void shouldDeclareAnnotationOnMethodsInEntityClass() {
        methods()
            .that()
            .areDeclaredIn(CardGame.class)
            .and()
            .haveNameMatching("\\$\\$_hibernate_.*")
            .should()
            .beAnnotatedWith("org.hibernate.bytecode.enhance.internal.bytebuddy.CodeTemplates$SuppressFBWarnings")
            .check(javaClasses());
    }

    @Test
    public void shouldDeclareAnnotationOnFieldsInEmbeddedClass() {
        fields()
            .that()
            .areDeclaredIn(Component.class)
            .and()
            .haveNameMatching("\\$\\$_hibernate_.*")
            .should()
            .beAnnotatedWith("org.hibernate.bytecode.enhance.internal.bytebuddy.CodeTemplates$SuppressFBWarnings")
            .check(javaClasses());
    }

    @Test
    public void shouldDeclareAnnotationOnMethodsInEmbeddedClass() {
        methods()
            .that()
            .areDeclaredIn(Component.class)
            .and()
            .haveNameMatching("\\$\\$_hibernate_.*")
            .should()
            .beAnnotatedWith("org.hibernate.bytecode.enhance.internal.bytebuddy.CodeTemplates$SuppressFBWarnings")
            .check(javaClasses());
    }

    private JavaClasses javaClasses() {
        String path = System.getProperty("java.io.tmpdir") + File.separator;

        return new ClassFileImporter()
            .importPath(path + "/org/hibernate/orm/test/bytecode/enhance/internal/bytebuddy");
    }

    @Embeddable
    public static class Component {
        @Column(name = "first_player_token")
        private String component;

        public Component() {
        }

        private Component(String component) {
            this.component = component;
        }

        public String getComponent() {
            return component;
        }

        public void setComponent(String component) {
            this.component = component;
        }
    }

    @Entity(name = "CardGame")
    public static class CardGame {

        @Id
        private String id;
        private String name;

        @Embedded
        private Component firstPlayerToken;

        public CardGame() {
        }

        private CardGame(String id, String name) {
            this.id = id;
            this.name = name;
            this.firstPlayerToken = createEmbeddedValue(name);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
            this.firstPlayerToken = createEmbeddedValue(name);
        }

        public Component getFirstPlayerToken() {
            return firstPlayerToken;
        }

        public void setFirstPlayerToken(Component firstPlayerToken) {
            this.firstPlayerToken = firstPlayerToken;
        }

        private Component createEmbeddedValue(String name) {
            return new Component(name + " first player token");
        }
    }

}
