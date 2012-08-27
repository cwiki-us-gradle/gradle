/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine;

import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.ResolvedModuleVersionResult;
import org.gradle.api.internal.artifacts.ResolvedConfigurationIdentifier;
import org.gradle.api.internal.artifacts.result.DefaultResolutionResult;
import org.gradle.api.internal.artifacts.result.DefaultResolvedDependencyResult;
import org.gradle.api.internal.artifacts.result.DefaultResolvedModuleVersionResult;

import java.util.*;

/**
 * by Szczepan Faber, created at: 7/26/12
 */
public class ResolutionResultBuilder implements ResolvedConfigurationListener {

    private ResolvedConfigurationIdentifier root;

    private Map<ModuleVersionIdentifier, Set<DependencyResult>> deps
            = new LinkedHashMap<ModuleVersionIdentifier, Set<DependencyResult>>();

    public void start(ResolvedConfigurationIdentifier root) {
        this.root = root;
    }

    public void resolvedConfiguration(ResolvedConfigurationIdentifier id, Collection<DependencyResult> dependencies) {
        if (!deps.containsKey(id.getId())) {
            deps.put(id.getId(), new LinkedHashSet<DependencyResult>());
        }
        deps.get(id.getId()).addAll(dependencies);
    }

    public ResolutionResult getResult() {
        return new DefaultResolutionResult(buildGraph());
    }

    private ResolvedModuleVersionResult buildGraph() {
        DefaultResolvedModuleVersionResult rootResult = new DefaultResolvedModuleVersionResult(root.getId());

        Set<ResolvedModuleVersionResult> visited = new HashSet<ResolvedModuleVersionResult>();

        return buildNode(rootResult, visited);
    }

    private ResolvedModuleVersionResult buildNode(DefaultResolvedModuleVersionResult node, Set<ResolvedModuleVersionResult> visited) {
        if (!visited.add(node)) {
            return node;
        }

        Set<DependencyResult> theDeps = deps.get(node.getId());
        for (DependencyResult d: theDeps) {
            //TODO SF this casting stuff can be avoided if we pass some intermediate type from GraphBuilder
            if (d instanceof DefaultResolvedDependencyResult) {
                DefaultResolvedModuleVersionResult selected = ((DefaultResolvedDependencyResult) d).getSelected();
                buildNode(selected, visited);
                //TODO SF dependee should be edge, not node
                selected.addDependee(node);
            }
            node.addDependency(d);
        }

        return node;
    }
}
