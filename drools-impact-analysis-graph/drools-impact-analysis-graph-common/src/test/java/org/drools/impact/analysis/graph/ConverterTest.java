package org.drools.impact.analysis.graph;

import java.util.List;

import org.drools.impact.analysis.model.AnalysisModel;
import org.drools.impact.analysis.parser.ModelBuilder;
import org.drools.impact.analysis.parser.domain.Address;
import org.drools.impact.analysis.parser.domain.Person;
import org.junit.Test;

public class ConverterTest {

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

        System.out.println(nodeList);
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

        System.out.println(nodeList);
    }
}
