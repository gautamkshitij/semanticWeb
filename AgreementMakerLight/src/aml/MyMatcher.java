/******************************************************************************
 * Copyright 2013-2016 LASIGE                                                  *
 *                                                                             *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may     *
 * not use this file except in compliance with the License. You may obtain a   *
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
 *                                                                             *
 * Unless required by applicable law or agreed to in writing, software         *
 * distributed under the License is distributed on an "AS IS" BASIS,           *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
 * See the License for the specific language governing permissions and         *
 * limitations under the License.                                              *
 *                                                                             *
 *******************************************************************************
 * Test-runs AgreementMakerLight in Eclipse.                                   *
 *                                                                             *
 * @author Daniel Faria                                                        *
 ******************************************************************************/
package aml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.match.*;
import aml.filter.Selector;
import aml.settings.EntityType;
import aml.settings.SelectionType;
import aml.util.ISub;
import aml.util.StopList;
import aml.util.StringParser;

public class MyMatcher extends AbstractInstanceMatcher {
    //Attributes
    private Set<String> stopSet;

    //Constructors
    public MyMatcher() {
        this(true);
    }

    public MyMatcher(boolean useWordNet) {
        super(useWordNet);
        stopSet = StopList.read();
    }

    //Public Methods
    public Alignment match(EntityType e, double threshold) throws UnsupportedEntityTypeException {
        if (!e.equals(EntityType.INDIVIDUAL))
            throw new UnsupportedEntityTypeException(e.toString());
        int classId = AML.getInstance().getSource().getLexicon().getBestEntity(EntityType.CLASS, "topic", true);


        Alignment a = new Alignment();

        //For each combination of instances, perform a name match
        for (Integer s : sourceInd) {
            if (!rels.belongsToClass(s, classId))
                continue;
            for (Integer t : targetInd) {
                double sim = nameSimilarity(s, t, true);
                if (sim >= threshold) {
                    a.add(s, t, sim);
                }
            }
        }

        //Perform selection
        Selector s = new Selector(threshold, SelectionType.PERMISSIVE);
        a = s.filter(a);

        return a;
    }

    //Private Methods
    protected double nameSimilarity(int i1, int i2, boolean useWordNet) {


        HashMap<String, Double> similarityMeasures = new HashMap<>();

        double names_Similarity = 0.0;
        double synonym_Similarity = 0.0;
        double acronym_Similarity = 0.0;
        double multiword_Similarity = 0.0;
        double commonWordsInArticles_similarity = 0.0;
        double partOfSPeech_similarity = 0.0;
        double definition_similarity = 0.0;

        for (String n1 : sLex.getNames(i1)) {
            if (n1.length() != 0 && n1 != null) {
                for (String n2 : tLex.getNames(i2)) {
                    if (n2.length() != 0 && n2 != null) {


                        names_Similarity = Math.max(names_Similarity, nameSimilarity(n1, n2, useWordNet));


                    }
                }
            }
        }

        similarityMeasures.put("NamesSimilarity", names_Similarity);
        similarityMeasures.put("synonymSimilarity", synonym_Similarity);
        similarityMeasures.put("acronymSimilarity", acronym_Similarity);
        similarityMeasures.put("multiwordSimilarity", multiword_Similarity);
        similarityMeasures.put("commonWordsSimilarity", commonWordsInArticles_similarity);


        return names_Similarity;
    }

    protected double acronymMatcher(String n1, String n2) {


        return 0.0;
    }

    protected double nameSimilarity(String n1, String n2, boolean useWordNet) {
        //Check whether the names are equal
        if (n1.equals(n2))
            return 1.0;

        //Since we cannot use string or word similarity on formulas
        //if the names are (non-equal) formulas their similarity is zero
        if (StringParser.isFormula(n1) || StringParser.isFormula(n2))
            return 0.0;

        //Compute the String similarity
        double stringSim = ISub.stringSimilarity(n1, n2);

        //Compute the String similarity after removing stop words
        String n1S = n1;
        String n2S = n2;
        for (String s : stopSet) {
            n1S = n1S.replace(s, "").trim();
            n2S = n2S.replace(s, "").trim();
        }
        stringSim = Math.max(stringSim, ISub.stringSimilarity(n1S, n2S) * 0.95);

        //Compute the Word similarity (ignoring stop words)

        double wordSim = 0.0;

        //Split the source name into words
        String[] sW = n1.split(" ");
        HashSet<String> n1Words = new HashSet<String>();
        for (String s : sW)
            if (!stopSet.contains(s))
                n1Words.add(s);

        String[] tW = n2.split(" ");
        HashSet<String> n2Words = new HashSet<String>();
        for (String s : tW)
            if (!stopSet.contains(s))
                n2Words.add(s);

        //Do a weighted Jaccard, where words are weighted by length
        double total = 0.0;
        for (String s : n1Words) {
            if (n2Words.contains(s))
                wordSim += s.length();
            else
                total += s.length();
        }
        for (String s : n2Words)
            total += s.length();
        wordSim /= total;


        return Math.max(stringSim, wordSim);
    }


    //Main Method
    public static void main(String[] args) throws Exception {
        //Path to input ontology files (edit manually)
        String sourcePath = "/Users/kshitijgautam/IdeaProjects/AML-Project/AgreementMakerLight/data/sabine_linking/sabine_source.owl";
        String targetPath = "/Users/kshitijgautam/IdeaProjects/AML-Project/AgreementMakerLight/data/sabine_linking/sabine_target.owl";
        String referencePath = "/Users/kshitijgautam/IdeaProjects/AML-Project/AgreementMakerLight/data/sabine_linking/refalign.rdf";

        //Path to save output alignment (edit manually, or leave blank for no evaluation)
        String outputPath = "";

        //Get AML instance and open ontologies
        long time = System.currentTimeMillis();
        AML aml = AML.getInstance();
        aml.openOntologies(sourcePath, targetPath);

        //Run instance matcher and save output alignment
        System.out.println("Running Instance Matcher");

        //Set threshold
        double threshold = 0.9;

        //Matching Algorithm
        MyMatcher mm = new MyMatcher();
        Alignment a = mm.match(EntityType.INDIVIDUAL, threshold);

        aml.setAlignment(a);

        time = (System.currentTimeMillis() - time) / 1000;
        System.out.println("Finished in " + time + " seconds");

        //Open the reference alignment and evaluate
        aml.openReferenceAlignment(referencePath);
        Alignment ref = aml.getReferenceAlignment();
        aml.evaluate();
        System.out.println(aml.getEvaluation());

    }
}