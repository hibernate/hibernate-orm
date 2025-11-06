/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2018-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.api.reveng;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.util.ReflectionUtil;
import org.jboss.logging.Logger;

public class RevengStrategyFactory {
    final private static Logger log = Logger.getLogger( RevengStrategyFactory.class );

    private static RevengStrategy createDefaultStrategyInstance() {
        return new DefaultStrategy();
    }

    private static RuntimeException createExceptionBecauseOfMissingConstructors(Class<?> revengClass) {
        return new RuntimeException("A strategy of class '" + revengClass.getName() + "' could not be created. " +
                "No matching constructor found: '" + revengClass.getSimpleName() + "()' or " +
                "'" + revengClass.getSimpleName() + "(" + RevengStrategy.class.getName() + ")'");
    }

    public static RevengStrategy createReverseEngineeringStrategy(
            String revengClassName, RevengStrategy delegate) {
        if (revengClassName == null) {
            return delegate == null ? createDefaultStrategyInstance() : delegate;
        }

        try {
            Class<?> revengClass = ReflectionUtil.classForName(revengClassName);

            if (delegate == null) {
                try {
                    try {
                        Constructor<?> revengConstructor = revengClass.getConstructor();
                        return (RevengStrategy) revengConstructor.newInstance();
                    }
                    catch (NoSuchMethodException e1) {
                        try {
                            Constructor<?> revengConstructor = revengClass.getConstructor(RevengStrategy.class);
                            return (RevengStrategy) revengConstructor.newInstance(createDefaultStrategyInstance());
                        }
                        catch (NoSuchMethodException e2) {
                            throw createExceptionBecauseOfMissingConstructors(revengClass);
                        }
                    }
                }
                catch (IllegalAccessException | InstantiationException | InvocationTargetException | IllegalArgumentException e) {
                    throw new RuntimeException("A strategy of class '" + revengClassName + "' could not be created", e);
                }
            }

            try {
                try {
                    Constructor<?> revengConstructor = revengClass.getConstructor(RevengStrategy.class);
                    return (RevengStrategy) revengConstructor.newInstance(delegate);
                }
                catch (NoSuchMethodException e1) {
                    log.warn("Delegating strategy given, but no matching constructor is available on class '" +
                            revengClassName + "'. The delegating strategy will be ignored!");
                    try {
                        Constructor<?> revengConstructor = revengClass.getConstructor();
                        return (RevengStrategy) revengConstructor.newInstance();
                    }
                    catch (NoSuchMethodException e2) {
                        throw createExceptionBecauseOfMissingConstructors(revengClass);
                    }
                }
            }
            catch (IllegalAccessException | InstantiationException | InvocationTargetException | IllegalArgumentException e) {
                throw new RuntimeException("A strategy of class '" + revengClassName + "' could not be created", e);
            }
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("A strategy of class '" + revengClassName + "' could not be created", e);
        }
    }

    public static RevengStrategy createReverseEngineeringStrategy( String revengClassName, File[] revengFiles) {
        RevengStrategy result = null;
        if (revengFiles != null && revengFiles.length > 0) {
            OverrideRepository overrideRepository = new OverrideRepository();
            for (File file : revengFiles) {
                overrideRepository.addFile(file);
            }
            result = overrideRepository.getReverseEngineeringStrategy(createDefaultStrategyInstance());
        }

        return createReverseEngineeringStrategy(revengClassName, result);
    }

    public static RevengStrategy createReverseEngineeringStrategy(String revengClassName) {
        return createReverseEngineeringStrategy(revengClassName, (RevengStrategy) null);
    }

    public static RevengStrategy createReverseEngineeringStrategy() {
        return createDefaultStrategyInstance();
    }
}
