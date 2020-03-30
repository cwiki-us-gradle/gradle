/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.provider;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ProviderTestUtil {
    public static <T> ProviderInternal<T> withNoValue() {
        return Providers.notDefined();
    }

    public static <T> ProviderInternal<T> withValues(T... values) {
        assert values.length > 0;
        return new TestProvider<>((Class<T>) values[0].getClass(), Arrays.asList(values), null);
    }

    public static <T> ProviderInternal<T> withProducer(Class<T> type, Task producer) {
        return new TestProvider<>(type, Collections.emptyList(), producer);
    }

    private static class TestProvider<T> extends AbstractMinimalProvider<T> {
        final Class<T> type;
        final Iterator<T> values;
        final Task producer;

        TestProvider(Class<T> type, List<T> values, Task producer) {
            this.producer = producer;
            this.values = values.iterator();
            this.type = type;
        }

        @Override
        public Class<T> getType() {
            return type;
        }

        @Override
        public void visitProducerTasks(Action<? super Task> visitor) {
            if (producer != null) {
                visitor.execute(producer);
            }
        }

        @Override
        public boolean maybeVisitBuildDependencies(TaskDependencyResolveContext context) {
            if (producer != null) {
                context.add(producer);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean isPresent() {
            return values.hasNext();
        }

        @Override
        protected Value<? extends T> calculateOwnValue() {
            return values.hasNext() ? Value.of(values.next()) : Value.missing();
        }
    }
}
