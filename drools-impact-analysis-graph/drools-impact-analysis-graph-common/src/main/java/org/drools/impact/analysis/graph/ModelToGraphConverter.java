/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.impact.analysis.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.drools.impact.analysis.model.AnalysisModel;
import org.drools.impact.analysis.model.Package;
import org.drools.impact.analysis.model.Rule;
import org.drools.impact.analysis.model.left.Constraint;
import org.drools.impact.analysis.model.left.LeftHandSide;
import org.drools.impact.analysis.model.left.Pattern;
import org.drools.impact.analysis.model.right.ConsequenceAction;
import org.drools.impact.analysis.model.right.ModifiedProperty;
import org.drools.impact.analysis.model.right.ModifyAction;
import org.drools.impact.analysis.model.right.RightHandSide;

public class ModelToGraphConverter {

    private static final String NO_REACT = "this";

    public List<Node> toNodeList(AnalysisModel model) {
        Map<String, Node> nodeMap = new HashMap<>();
        Map<Class<?>, Map<String, Set<Rule>>> reactFactMap = new HashMap<>();

        List<Package> packages = model.getPackages();
        for (Package pkg : packages) {
            String pkgName = pkg.getName();
            List<Rule> rules = pkg.getRules();
            for (Rule rule : rules) {
                Node node = new Node(pkgName, rule.getName());
                nodeMap.put(node.getFqdn(), node);

                LeftHandSide lhs = rule.getLhs();
                List<Pattern> patterns = lhs.getPatterns();
                for (Pattern pattern : patterns) {
                    Class<?> patternClass = pattern.getPatternClass();
                    Map<String, Set<Rule>> reactFieldMap = reactFactMap.computeIfAbsent(patternClass, k -> new HashMap<>());
                    Collection<String> reactOnFields = pattern.getReactOnFields();
                    if (reactOnFields.size() == 0) {
                        // TODO: confirm mask logic (No constraint vs Failed to analyze property reactivity)
                        Set<Rule> ruleSet = reactFieldMap.computeIfAbsent(NO_REACT, k -> new HashSet<>());
                        ruleSet.add(rule);
                    } else {
                        for (String field : reactOnFields) {
                            Set<Rule> ruleSet = reactFieldMap.computeIfAbsent(field, k -> new HashSet<>());
                            ruleSet.add(rule);
                        }
                    }
                }
            }
        }

        for (Package pkg : packages) {
            String pkgName = pkg.getName();
            List<Rule> rules = pkg.getRules();
            for (Rule rule : rules) {
                String ruleName = rule.getName();
                RightHandSide rhs = rule.getRhs();
                List<ConsequenceAction> actions = rhs.getActions();
                for (ConsequenceAction action : actions) {
                    switch (action.getType()) {
                        case INSERT:
                            processInsert(nodeMap, reactFactMap, pkgName, ruleName, action);
                            break;
                        case DELETE:
                            processDelete(nodeMap, reactFactMap, pkgName, ruleName, action);
                            break;
                        case MODIFY:
                            processModify(nodeMap, reactFactMap, pkgName, ruleName, (ModifyAction) action);
                            break;
                    }
                }
            }
        }

        return nodeMap.values().stream().collect(Collectors.toList());
    }

    private void processInsert(Map<String, Node> nodeMap, Map<Class<?>, Map<String, Set<Rule>>> reactFactMap, String pkgName, String ruleName, ConsequenceAction action) {
        // TODO: consider not()
        Class<?> insertedClass = action.getActionClass();
        Map<String, Set<Rule>> reactFieldMap = reactFactMap.get(insertedClass);
        // all rules which react to the fact
        Set<Rule> reactedRules = reactFieldMap.values().stream().flatMap(ruleSet -> ruleSet.stream()).collect(Collectors.toSet());
        Node source = nodeMap.get(fqdn(pkgName, ruleName));
        for (Rule reactedRule : reactedRules) {
            Node target = nodeMap.get(fqdn(pkgName, reactedRule.getName()));
            Node.linkNodes(source, target, Link.Type.POSITIVE);
        }
    }

    private void processDelete(Map<String, Node> nodeMap, Map<Class<?>, Map<String, Set<Rule>>> reactFactMap, String pkgName, String ruleName, ConsequenceAction action) {
        // TODO: consider exists()
        Class<?> deletedClass = action.getActionClass();
        Map<String, Set<Rule>> reactFieldMap = reactFactMap.get(deletedClass);
        // all rules which react to the fact
        Set<Rule> reactedRules = reactFieldMap.values().stream().flatMap(ruleSet -> ruleSet.stream()).collect(Collectors.toSet());
        Node source = nodeMap.get(fqdn(pkgName, ruleName));
        for (Rule reactedRule : reactedRules) {
            Node target = nodeMap.get(fqdn(pkgName, reactedRule.getName()));
            Node.linkNodes(source, target, Link.Type.NEGATIVE);
        }
    }

    private void processModify(Map<String, Node> nodeMap, Map<Class<?>, Map<String, Set<Rule>>> reactFactMap, String pkgName, String ruleName, ModifyAction action) {
        // TODO: consider exists()/not()
        Node source = nodeMap.get(fqdn(pkgName, ruleName));

        Class<?> modifiedClass = action.getActionClass();
        Map<String, Set<Rule>> reactFieldMap = reactFactMap.get(modifiedClass);
        List<ModifiedProperty> modifiedProperties = action.getModifiedProperties();
        for (ModifiedProperty modifiedProperty : modifiedProperties) {
            String property = modifiedProperty.getProperty();
            Set<Rule> rules = reactFieldMap.get(property);
            for (Rule reactedRule : rules) {
                List<Constraint> constraints = reactedRule.getLhs().getPatterns().stream()
                                                          .filter(pattern -> pattern.getPatternClass() == modifiedClass)
                                                          .flatMap(pattern -> pattern.getConstraints().stream())
                                                          .filter(constraint -> constraint.getProperty().equals(property))
                                                          .collect(Collectors.toList());
                if (constraints.size() == 0) {
                    // This rule is reactive to the property but cannot find its constraint. Exception or UNKNOWN?
                    throw new RuntimeException("This rule [" + reactedRule.getName() + "] is reactive to the property [" + property + "] " +
                                               "but cannot find its constraint : \n" + reactedRule);
                }
                Link.Type combinedLinkType = Link.Type.UNKNOWN; // If constraints contain at least one POSITIVE, we consider it's POSITIVE.
                for (Constraint constraint : constraints) {
                    Link.Type linkType = linkType(constraint, modifiedProperty);
                    if (linkType == Link.Type.POSITIVE) {
                        combinedLinkType = Link.Type.POSITIVE;
                        break;
                    } else if (linkType == Link.Type.NEGATIVE) {
                        combinedLinkType = Link.Type.NEGATIVE; // overwrite if it's UNKNOWN
                    } else {
                        combinedLinkType = linkType; // UNKNOWN
                    }
                }
                Node target = nodeMap.get(fqdn(pkgName, reactedRule.getName()));
                Node.linkNodes(source, target, combinedLinkType);
            }
        }
    }

    private Link.Type linkType(Constraint constraint, ModifiedProperty modifiedProperty) {
        Object value = constraint.getValue();
        Object modifiedValue = modifiedProperty.getValue();

        // If value and/or modifiedValue are not literal (e.g. age > $a), return Link.Type.UNKNOWN
        // Check with Mario if we can distinguish literal or not on the model side

        switch (constraint.getType()) {
            case EQUAL:
                if (modifiedValue.equals(value)) {
                    return Link.Type.POSITIVE;
                } else {
                    return Link.Type.NEGATIVE;
                }
            case NOT_EQUAL:
                if (!modifiedValue.equals(value)) {
                    return Link.Type.POSITIVE;
                } else {
                    return Link.Type.NEGATIVE;
                }
            case GREATER_THAN:
                if (((Comparable) modifiedValue).compareTo((Comparable) value) > 0) {
                    return Link.Type.POSITIVE;
                } else {
                    return Link.Type.NEGATIVE;
                }
            case GREATER_OR_EQUAL:
                if (((Comparable) modifiedValue).compareTo((Comparable) value) >= 0) {
                    return Link.Type.POSITIVE;
                } else {
                    return Link.Type.NEGATIVE;
                }
            case LESS_THAN:
                if (((Comparable) modifiedValue).compareTo((Comparable) value) < 0) {
                    return Link.Type.POSITIVE;
                } else {
                    return Link.Type.NEGATIVE;
                }
            case LESS_OR_EQUAL:
                if (((Comparable) modifiedValue).compareTo((Comparable) value) <= 0) {
                    return Link.Type.POSITIVE;
                } else {
                    return Link.Type.NEGATIVE;
                }
            case RANGE:
                // TODO:
                break;
            case UNKNOWN:
                break;
        }
        return Link.Type.UNKNOWN;
    }

    private static String fqdn(String pckageName, String ruleName) {
        return pckageName + "." + ruleName;
    }
}
