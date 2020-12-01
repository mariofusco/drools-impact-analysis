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

package org.drools.impact.analysis.graph.graphviz;

import java.util.ArrayList;
import java.util.List;

import org.drools.impact.analysis.graph.Link;
import org.drools.impact.analysis.graph.ModelToGraphConverter;
import org.drools.impact.analysis.graph.Node;
import org.drools.impact.analysis.model.AnalysisModel;
import org.drools.impact.analysis.model.Rule;
import org.drools.impact.analysis.parser.ModelBuilder;
import org.drools.impact.analysis.parser.domain.Address;
import org.drools.impact.analysis.parser.domain.Person;
import org.junit.Test;

public class BasicGraphTest {

    @Test
    public void testBasicGraph() {
        Node node1 = new Node(new Rule("org.example", "rule1", "dummy"));
        Node node2 = new Node(new Rule("org.example", "rule2", "dummy"));
        Node node3 = new Node(new Rule("org.example", "rule3", "dummy"));
        Node node4 = new Node(new Rule("org.example", "rule4", "dummy"));
        Node node5 = new Node(new Rule("org.example", "rule5", "dummy"));

        Node.linkNodes(node1, node2, Link.Type.POSITIVE);
        Node.linkNodes(node1, node3, Link.Type.NEGATIVE);
        Node.linkNodes(node2, node4, Link.Type.UNKNOWN);
        Node.linkNodes(node3, node5, Link.Type.POSITIVE);

        List<Node> nodeList = new ArrayList<>();
        nodeList.add(node1);
        nodeList.add(node2);
        nodeList.add(node3);
        nodeList.add(node4);
        nodeList.add(node5);

        GraphGenerator generator = new GraphGenerator("basic");
        generator.generatePng(nodeList);
    }

    @Test
    public void test3Rules() {
        String str =
                "package mypkg;\n" +
                     "import " + Person.class.getCanonicalName() + ";" +
                     "rule R1 when\n" +
                     "  $p : Person(name == \"Mario\")\n" +
                     "then\n" +
                     "  modify($p) { setAge( 18 ) };" +
                     "  insert(\"Done\");\n" +
                     "end\n" +
                     "rule R2 when\n" +
                     "  $p : Person(age > 15)\n" +
                     "then\n" +
                     "end\n" +
                     "rule R3 when\n" +
                     "  $p : String(this == \"Done\")\n" +
                     "then\n" +
                     "end\n";

        AnalysisModel analysisModel = new ModelBuilder().build(str);
        //System.out.println(analysisModel);

        ModelToGraphConverter converter = new ModelToGraphConverter();
        List<Node> nodeList = converter.toNodeList(analysisModel);
        GraphGenerator generator = new GraphGenerator("3rules");
        generator.generatePng(nodeList);
    }

    @Test
    public void test5Rules() {
        String str =
                "package mypkg;\n" +
                     "import " + Person.class.getCanonicalName() + ";" +
                     "import " + Address.class.getCanonicalName() + ";" +
                     "rule R1 when\n" +
                     "  $p : Person(name == \"Mario\")\n" +
                     "then\n" +
                     "  modify($p) { setAge( 18 ) };" +
                     "end\n" +
                     "rule R2 when\n" +
                     "  $p : Person(age > 15)\n" +
                     "then\n" +
                     "  insert(new Address(\"Milan\"));" +
                     "end\n" +
                     "rule R3 when\n" +
                     "  $p : Person(age < 15)\n" +
                     "then\n" +
                     "  insert(new Address(\"Milan\"));" +
                     "end\n" +
                     "rule R4 when\n" +
                     "  $a : Address()\n" +
                     "then\n" +
                     "end\n" +
                     "rule R5 when\n" +
                     "  $i : Integer()\n" +
                     "then\n" +
                     "end\n";

        AnalysisModel analysisModel = new ModelBuilder().build(str);
        //System.out.println(analysisModel);

        ModelToGraphConverter converter = new ModelToGraphConverter();
        List<Node> nodeList = converter.toNodeList(analysisModel);
        GraphGenerator generator = new GraphGenerator("5rules");
        generator.generatePng(nodeList);
    }

    @Test
    public void testBeta() {
        String str =
                "package mypkg;\n" +
                     "import " + Person.class.getCanonicalName() + ";" +
                     "rule R1 when\n" +
                     "  $p : Person(name == \"Mario\")\n" +
                     "then\n" +
                     "  modify($p) { setAge( 18 ) };" +
                     "end\n" +
                     "rule R2 when\n" +
                     "  $a : Integer()\n" +
                     "  $p2 : Person(age > $a)\n" +
                     "then\n" +
                     "end\n";

        AnalysisModel analysisModel = new ModelBuilder().build(str);
        System.out.println(analysisModel);

        ModelToGraphConverter converter = new ModelToGraphConverter();
        List<Node> nodeList = converter.toNodeList(analysisModel);
        GraphGenerator generator = new GraphGenerator("beta");
        generator.generatePng(nodeList);
    }
}
